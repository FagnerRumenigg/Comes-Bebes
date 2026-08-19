package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.request.CreateIngredientRequest;
import org.application.controller.publication.request.CreatePublicationRequest;
import org.application.controller.publication.request.CreateRecipeRequest;
import org.application.controller.publication.request.UpdatePublicationRequest;
import org.application.controller.publication.request.CreateMyVersionRequest;
import org.application.controller.publication.request.CreatePublicationUploadRequest;
import org.application.dto.StoredImage;
import org.application.model.Follow;
import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.Recipe;
import org.application.model.RecipeIngredient;
import org.application.model.PublicationOrigin;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.repository.FollowRepository;
import org.application.repository.PublicationOriginRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.RecipeIngredientRepository;
import org.application.repository.RecipeRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.storage.ImageStorage;
import org.application.service.validation.ImageValidatorClient;
import org.application.model.PublicationImageCheck;
import org.application.repository.PublicationImageCheckRepository;
import org.application.util.StringNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.application.model.PublicationVisibility;

@Service
@RequiredArgsConstructor
public class PublicationService {
    private static final Logger log = LoggerFactory.getLogger(PublicationService.class);
    private static final String FOLLOWED_USER_PUBLISHED = "FOLLOWED_USER_PUBLISHED";

    private final PublicationRepository publicationRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;
    private final StringNormalizer stringNormalizer;
    private final Clock clock;
    private final ImageValidatorClient imageValidatorClient;
    private final PublicationImageCheckRepository publicationImageCheckRepository;
    private final ZoneId applicationZoneId;
    private final FollowRepository followRepository;
    private final UserNotificationRepository notificationRepository;
    private final PublicationRateLimiter publicationRateLimiter;
    private final TagService tagService;

    private final PublicationOriginRepository publicationOriginRepository;

    @Transactional
    public Publication createMyVersion(UUID sourcePublicationId, CreateMyVersionRequest request) {
        return createMyVersion(sourcePublicationId, request, request.authorId());
    }

    @Transactional
    public Publication createMyVersion(UUID sourcePublicationId, CreateMyVersionRequest request, UUID authorId) {
        return createMyVersion(sourcePublicationId, request, authorId, imageStorage.store(request.imageUrl()));
    }

    @Transactional
    public Publication createMyVersion(UUID sourcePublicationId, CreateMyVersionRequest request, UUID authorId, StoredImage image) {
        publicationRateLimiter.recordAttempt(authorId);
        Publication source = findActive(sourcePublicationId);
        if (source.getTitle() == null || source.getTitle().isBlank()) {
            throw new InvalidOperationException("SOURCE_RECIPE_TITLE_REQUIRED", "A receita original precisa possuir um título.");
        }
        Recipe sourceRecipe = recipeRepository.findByPublicationIdAndDeletedAtIsNull(sourcePublicationId)
                .orElseThrow(() -> new ResourceNotFoundException("SOURCE_RECIPE_NOT_FOUND", "Receita original não encontrada."));
        userRepository.findByIdAndStatus(authorId, org.application.model.UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Autor não encontrado."));
        validateRecipe(PublicationType.RECIPE, request.recipe());

        UUID derivedId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        Publication derived = Publication.builder()
                .id(derivedId)
                .authorId(authorId)
                .type(PublicationType.MY_VERSION)
                .visibility(parseVisibility(request.visibility()))
                .title(source.getTitle() + " — " + request.titleSuffix())
                .description(request.changeSummary())
                .gcsBucket(image.bucket())
                .gcsObjectName(image.objectName())
                .gcsGeneration(image.generation())
                .imageFormat(image.format())
                .imageSizeBytes(image.sizeBytes())
                .imageWidth(image.width())
                .imageHeight(image.height())
                .status(PublicationStatus.ACTIVE)
                .publishedAt(now)
                .build();
        Publication saved = publicationRepository.save(derived);
        saveRecipe(saved.getId(), request.recipe());
        publicationOriginRepository.save(PublicationOrigin.builder()
                .derivedPublicationId(saved.getId())
                .sourceRecipeId(sourceRecipe.getPublicationId())
                .sourceTitleSnapshot(source.getTitle())
                .titleSuffix(request.titleSuffix())
                .changeSummary(request.changeSummary())
                .build());
        tagService.attachToPublication(saved.getId(), tagService.resolveOrCreate(request.tags(), authorId));
        notifyFollowers(saved);
        return saved;
    }

    @Transactional
    public Publication create(CreatePublicationRequest request) {
        return create(request, request.authorId());
    }

    @Transactional
    public Publication create(CreatePublicationRequest request, UUID authorId) {
        return create(request, authorId, imageStorage.store(request.imageUrl()));
    }

    @Transactional
    public Publication create(CreatePublicationRequest request, UUID authorId, StoredImage image) {
        publicationRateLimiter.recordAttempt(authorId);
        PublicationType type = parseType(request.type());
        PublicationVisibility visibility = parseVisibility(request.visibility());
        validateRecipe(type, request.recipe());
        if (type == PublicationType.RECIPE && (request.title() == null || request.title().isBlank())) {
            throw new InvalidOperationException("RECIPE_TITLE_REQUIRED", "Uma receita precisa informar um título.");
        }
        if (request.title() != null && request.title().length() > 150 && type != PublicationType.MY_VERSION) {
            throw new InvalidOperationException("PUBLICATION_TITLE_TOO_LONG", "O título de uma publicação comum deve ter no máximo 150 caracteres.");
        }

        userRepository.findByIdAndStatus(authorId, org.application.model.UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Autor não encontrado."));

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        Publication publication = Publication.builder()
                .id(UUID.randomUUID())
                .authorId(authorId)
                .type(type)
                .visibility(visibility)
                .title(request.title())
                .description(request.description())
                .gcsBucket(image.bucket())
                .gcsObjectName(image.objectName())
                .gcsGeneration(image.generation())
                .imageFormat(image.format())
                .imageSizeBytes(image.sizeBytes())
                .imageWidth(image.width())
                .imageHeight(image.height())
                .status(PublicationStatus.ACTIVE)
                .publishedAt(now)
                .build();

        Publication savedPublication = publicationRepository.save(publication);
        if (type == PublicationType.RECIPE) {
            saveRecipe(savedPublication.getId(), request.recipe());
        }
        tagService.attachToPublication(savedPublication.getId(), tagService.resolveOrCreate(request.tags(), authorId));
        notifyFollowers(savedPublication);
        return savedPublication;
    }

    /**
     * Dispara ao publicar (não ao editar) — quem segue o autor recebe uma notificação,
     * exceto quem desativou essa preferência. Busca segue-por-segue por não termos join
     * ad hoc entre Follow e User configurado; aceitável no volume atual de seguidores.
     */
    private void notifyFollowers(Publication publication) {
        List<Follow> followerRows = followRepository
                .findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(publication.getAuthorId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        if (followerRows.isEmpty()) return;

        List<UUID> followerIds = followerRows.stream().map(Follow::getFollowerId).toList();
        List<UserNotification> notifications = userRepository.findAllById(followerIds).stream()
                .filter(follower -> follower.getStatus() == UserStatus.ACTIVE && follower.isNotifyOnFollowedPublish())
                .map(follower -> UserNotification.builder()
                        .id(UUID.randomUUID())
                        .userId(follower.getId())
                        .type(FOLLOWED_USER_PUBLISHED)
                        .actorId(publication.getAuthorId())
                        .publicationId(publication.getId())
                        .build())
                .toList();
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional
    public Publication createUpload(CreatePublicationUploadRequest request, UUID authorId, byte[] content, String filename, String contentType) {
        var validation = imageValidatorClient.validate(content, filename, contentType);
        CreatePublicationRequest publicationRequest = new CreatePublicationRequest(
                authorId, request.type(), request.visibility(), request.title(), request.description(),
                "file://local/upload", request.recipe(), request.tags());
        Publication publication = create(publicationRequest, authorId,
                imageStorage.store(validation.imageBytes(), filename, validation.contentType()));
        saveApprovedImageCheck(publication, validation);
        applyPhotoTakenAt(publication, validation);
        return publication;
    }

    @Transactional
    public Publication createMyVersionUpload(UUID sourcePublicationId, CreateMyVersionRequest request, UUID authorId,
                                              byte[] content, String filename, String contentType) {
        var validation = imageValidatorClient.validate(content, filename, contentType);
        Publication publication = createMyVersion(sourcePublicationId, request, authorId,
                imageStorage.store(validation.imageBytes(), filename, validation.contentType()));
        saveApprovedImageCheck(publication, validation);
        applyPhotoTakenAt(publication, validation);
        return publication;
    }

    /**
     * O EXIF não garante fuso confiável, então o horário "ingênuo" devolvido pelo validador é
     * interpretado no fuso da aplicação (o mesmo já usado para exibir outras datas ao usuário).
     */
    private void applyPhotoTakenAt(Publication publication, ImageValidatorClient.ValidationResult validation) {
        OffsetDateTime photoTakenAt = parsePhotoTakenAt(validation.photoTakenAt());
        if (photoTakenAt == null) {
            return;
        }
        publication.recordPhotoTakenAt(photoTakenAt);
        publicationRepository.save(publication);
    }

    private OffsetDateTime parsePhotoTakenAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw).atZone(applicationZoneId).toOffsetDateTime();
        } catch (DateTimeParseException exception) {
            log.warn("event=photo_taken_at_parse_failed value={}", raw);
            return null;
        }
    }

    private void saveApprovedImageCheck(Publication publication, ImageValidatorClient.ValidationResult validation) {
        Double score = validation.foodScore();
        publicationImageCheckRepository.save(PublicationImageCheck.builder()
                .id(UUID.randomUUID())
                .publicationId(publication.getId())
                .checkType("FOOD")
                .provider("csatv2-v1")
                .attemptNumber((short) 1)
                .status("APPROVED")
                .confidence(score == null ? null : BigDecimal.valueOf(score))
                .checkedAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC))
                .build());
    }

    @Transactional(readOnly = true)
    public Publication findActive(UUID id) {
        return publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
    }

    @Transactional(readOnly = true)
    public Publication findAccessible(UUID id, UUID viewerId) {
        if (viewerId != null) {
            return findActive(id);
        }
        return publicationRepository.findByIdAndStatusAndVisibility(id, PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
    }

    private static final Set<PublicationType> ALL_PUBLICATION_TYPES = EnumSet.allOf(PublicationType.class);

    @Transactional(readOnly = true)
    public Page<Publication> feed(Pageable pageable) {
        return feed(pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<Publication> feed(Pageable pageable, UUID viewerId) {
        return feed(pageable, viewerId, null);
    }

    @Transactional(readOnly = true)
    public Page<Publication> feed(Pageable pageable, UUID viewerId, Set<PublicationType> types) {
        Set<PublicationType> effectiveTypes = types == null || types.isEmpty() ? ALL_PUBLICATION_TYPES : types;
        return viewerId == null
                ? publicationRepository.findByStatusAndVisibilityAndTypeInOrderByPublishedAtDescIdDesc(
                        PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC, effectiveTypes, pageable)
                : publicationRepository.findFeedForAuthenticatedViewerOrderByUnseenFirst(
                        PublicationStatus.ACTIVE, effectiveTypes, viewerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Publication> search(String title, Pageable pageable) {
        return publicationRepository.findByStatusAndVisibilityAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC, title, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Publication> search(String title, String ingredient, Pageable pageable) {
        String normalizedTitle = title == null || title.isBlank() ? "" : stringNormalizer.normalize(title);
        String normalizedIngredient = ingredient == null || ingredient.isBlank() ? "" : stringNormalizer.normalize(ingredient);
        if (normalizedTitle.isBlank() && normalizedIngredient.isBlank()) {
            throw new InvalidOperationException("SEARCH_TERM_REQUIRED", "Informe título ou ingrediente para pesquisar.");
        }
        return publicationRepository.searchByTitleOrIngredient(
                PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC, normalizedTitle, normalizedIngredient, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Publication> search(String title, String ingredient, Pageable pageable, UUID viewerId) {
        String normalizedTitle = title == null || title.isBlank() ? "" : stringNormalizer.normalize(title);
        String normalizedIngredient = ingredient == null || ingredient.isBlank() ? "" : stringNormalizer.normalize(ingredient);
        if (normalizedTitle.isBlank() && normalizedIngredient.isBlank()) {
            throw new InvalidOperationException("SEARCH_TERM_REQUIRED", "Informe título ou ingrediente para pesquisar.");
        }
        if (viewerId == null) {
            return search(title, ingredient, pageable);
        }
        return publicationRepository.searchByTitleOrIngredient(
                PublicationStatus.ACTIVE, null, normalizedTitle, normalizedIngredient, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Publication> profile(UUID authorId, Pageable pageable) {
        return publicationRepository.findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(authorId, PublicationStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Publication> profile(UUID authorId, Pageable pageable, UUID viewerId) {
        return viewerId == null
                ? publicationRepository.findByAuthorIdAndStatusAndVisibilityOrderByPublishedAtDescIdDesc(authorId, PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC, pageable)
                : profile(authorId, pageable);
    }

    @Transactional
    public Publication update(UUID id, UpdatePublicationRequest request) {
        return update(id, request, request.authorId());
    }

    @Transactional
    public Publication update(UUID id, UpdatePublicationRequest request, UUID actingUserId) {
        Publication publication = findActive(id);
        var actingUser = userRepository.findByIdAndStatus(actingUserId, org.application.model.UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOperationException("PUBLICATION_AUTHOR_REQUIRED", "Somente o autor pode alterar a publicação."));
        boolean isAuthor = actingUser.getId().equals(publication.getAuthorId());
        boolean isAdmin = actingUser.getRole() == org.application.model.UserRole.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new InvalidOperationException("PUBLICATION_AUTHOR_REQUIRED", "Somente o autor pode alterar a publicação.");
        }

        if (request.title() != null || request.description() != null) {
            publication.updateText(
                    request.title() == null ? publication.getTitle() : request.title(),
                    request.description() == null ? publication.getDescription() : request.description()
            );
        }
        if (request.visibility() != null) {
            publication.changeVisibility(parseVisibility(request.visibility()));
        }

        PublicationType targetType = request.type() == null ? publication.getType() : parseType(request.type());
        boolean convertsDishToRecipe = publication.getType() == PublicationType.DISH
                && targetType == PublicationType.RECIPE;
        boolean keepsRecipe = targetType == PublicationType.RECIPE || targetType == PublicationType.MY_VERSION;

        if (keepsRecipe) {
            if (convertsDishToRecipe && request.recipe() == null) {
                throw new InvalidOperationException("RECIPE_REQUIRED_FOR_CONVERSION", "A conversão para RECIPE precisa informar a receita.");
            }
            if (publication.getTitle() == null || publication.getTitle().isBlank()) {
                throw new InvalidOperationException("RECIPE_TITLE_REQUIRED", "Uma receita precisa informar um título.");
            }
            if (request.recipe() != null) {
                validateRecipe(PublicationType.RECIPE, request.recipe());
                saveOrUpdateRecipe(publication.getId(), request.recipe());
            }
            if (targetType == PublicationType.RECIPE) {
                publication.changeType(PublicationType.RECIPE);
            }
        } else if (targetType == PublicationType.DISH) {
            publication.changeType(PublicationType.DISH);
            deactivateRecipe(publication.getId());
        }

        if (isAdmin && !isAuthor) {
            publication.markEditedByAdmin(actingUser.getId(), OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
            log.info("event=publication_edited_by_admin publicationId={} adminId={} authorId={}",
                    id, actingUser.getId(), publication.getAuthorId());
        }

        if (request.tags() != null) {
            tagService.attachToPublication(publication.getId(), tagService.resolveOrCreate(request.tags(), actingUserId));
        }

        return publicationRepository.save(publication);
    }

    @Transactional
    public void remove(UUID id, UUID actingUserId) {
        Publication publication = findActive(id);
        var actingUser = userRepository.findByIdAndStatus(actingUserId, org.application.model.UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOperationException("PUBLICATION_AUTHOR_REQUIRED", "Somente o autor pode excluir a publicação."));
        boolean isAuthor = publication.getAuthorId().equals(actingUserId);
        boolean isAdmin = actingUser.getRole() == org.application.model.UserRole.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new InvalidOperationException("PUBLICATION_AUTHOR_REQUIRED", "Somente o autor pode excluir a publicação.");
        }
        publication.remove(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        publicationRepository.save(publication);
        if (isAdmin && !isAuthor) {
            log.info("event=publication_removed_by_admin publicationId={} adminId={} authorId={}",
                    id, actingUserId, publication.getAuthorId());
        }
    }

    private void saveRecipe(UUID publicationId, CreateRecipeRequest request) {
        Recipe recipe = recipeRepository.save(Recipe.builder()
                .publicationId(publicationId)
                .yieldQuantity(request.yieldQuantity())
                .yieldUnit(request.yieldUnit())
                .instructions(request.instructions())
                .build());

        List<RecipeIngredient> ingredients = request.ingredients().stream()
                .map(ingredient -> ingredient(ingredient, recipe.getPublicationId()))
                .toList();
        recipeIngredientRepository.saveAll(ingredients);
    }

    private void saveOrUpdateRecipe(UUID publicationId, CreateRecipeRequest request) {
        Recipe recipe = recipeRepository.findByPublicationIdAndDeletedAtIsNull(publicationId)
                .orElseGet(() -> Recipe.builder().publicationId(publicationId).build());
        recipe.update(request.yieldQuantity(), request.yieldUnit(), request.instructions());
        recipeRepository.save(recipe);
        deactivateIngredients(publicationId);
        recipeIngredientRepository.flush();
        recipeIngredientRepository.saveAll(request.ingredients().stream()
                .map(ingredient -> ingredient(ingredient, publicationId))
                .toList());
    }

    private void deactivateRecipe(UUID publicationId) {
        recipeRepository.findByPublicationIdAndDeletedAtIsNull(publicationId).ifPresent(recipe -> {
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
            recipe.deactivate(now);
            recipeRepository.save(recipe);
            deactivateIngredients(publicationId);
        });
    }

    private void deactivateIngredients(UUID recipeId) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        List<RecipeIngredient> activeIngredients = recipeIngredientRepository
                .findByRecipeIdAndDeletedAtIsNullOrderByPositionAsc(recipeId);
        activeIngredients.forEach(ingredient -> ingredient.delete(now));
        recipeIngredientRepository.saveAll(activeIngredients);
    }

    private RecipeIngredient ingredient(CreateIngredientRequest request, UUID recipeId) {
        return RecipeIngredient.builder()
                .id(UUID.randomUUID())
                .recipeId(recipeId)
                .position(request.position())
                .name(request.name())
                .quantity(request.quantity())
                .unit(request.unit())
                .note(request.note())
                .build();
    }

    private void validateRecipe(PublicationType type, CreateRecipeRequest recipe) {
        if (type == PublicationType.RECIPE && recipe == null) {
            throw new InvalidOperationException("RECIPE_REQUIRED", "Uma publicação RECIPE precisa informar a receita.");
        }
        if (type == PublicationType.DISH && recipe != null) {
            throw new InvalidOperationException("DISH_RECIPE_FORBIDDEN", "Uma publicação DISH não pode informar uma receita na criação.");
        }
        if (recipe != null && new HashSet<>(recipe.ingredients().stream().map(CreateIngredientRequest::position).toList()).size() != recipe.ingredients().size()) {
            throw new InvalidOperationException("DUPLICATE_INGREDIENT_POSITION", "As posições dos ingredientes não podem se repetir.");
        }
    }

    private PublicationType parseType(String type) {
        return PublicationType.valueOf(type);
    }

    private PublicationVisibility parseVisibility(String visibility) {
        return PublicationVisibility.valueOf(visibility);
    }

}
