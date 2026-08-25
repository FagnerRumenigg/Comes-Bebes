package org.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.application.model.Follow;
import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.model.Recipe;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import org.application.repository.FollowRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.PublicationImageCheckRepository;
import org.application.repository.RecipeRepository;
import org.application.repository.RecipeIngredientRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.repository.PublicationOriginRepository;
import org.application.service.storage.ImageStorage;
import org.application.service.validation.ImageValidatorClient;
import org.application.dto.StoredImage;
import org.application.controller.publication.request.CreatePublicationUploadRequest;
import org.application.util.StringNormalizer;
import org.application.controller.publication.request.CreatePublicationRequest;
import org.application.controller.publication.request.CreateMyVersionRequest;
import org.application.controller.publication.request.CreateIngredientRequest;
import org.application.controller.publication.request.CreateRecipeRequest;
import org.application.controller.publication.request.UpdatePublicationRequest;
import org.application.service.exception.RateLimitExceededException;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ImageStorage imageStorage;
    @Mock private StringNormalizer stringNormalizer;
    @Mock private Clock clock;
    @Mock private PublicationOriginRepository publicationOriginRepository;
    @Mock private ImageValidatorClient imageValidatorClient;
    @Mock private PublicationImageCheckRepository publicationImageCheckRepository;
    @Mock private FollowRepository followRepository;
    @Mock private UserNotificationRepository notificationRepository;
    @Mock private PublicationRateLimiter publicationRateLimiter;
    @Mock private TagService tagService;

    private ch.qos.logback.classic.Logger serviceLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUpLogging() {
        serviceLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PublicationService.class);
        appender = new ListAppender<>();
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDownLogging() {
        serviceLogger.detachAppender(appender);
    }

    @InjectMocks
    private PublicationService publicationService;

    @BeforeEach
    void setUpZone() {
        ReflectionTestUtils.setField(publicationService, "applicationZoneId", ZoneOffset.UTC);
    }

    @BeforeEach
    void setUpFollowDefaults() {
        lenient().when(followRepository.findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void shouldFindActivePublication() {
        UUID id = UUID.randomUUID();
        Publication publication = publication(id);
        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));

        assertThat(publicationService.findActive(id)).isSameAs(publication);
    }

    @Test
    void shouldRejectMissingPublication() {
        UUID id = UUID.randomUUID();
        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.findActive(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publicação não encontrada.");
    }

    @Test
    void shouldListFeedSearchAndProfile() {
        when(publicationRepository.findByStatusAndVisibilityAndTypeInOrderByPublishedAtDescIdDesc(any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(publicationRepository.findByStatusAndVisibilityAndTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(publicationRepository.findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(any(), any(), any()))
                .thenReturn(Page.empty());

        assertThat(publicationService.feed(Pageable.unpaged())).isEmpty();
        assertThat(publicationService.search("bolo", Pageable.unpaged())).isEmpty();
        assertThat(publicationService.profile(UUID.randomUUID(), Pageable.unpaged())).isEmpty();
    }

    @Test
    void shouldFilterFeedByTypeForVisitor() {
        when(publicationRepository.findByStatusAndVisibilityAndTypeInOrderByPublishedAtDescIdDesc(
                eq(PublicationStatus.ACTIVE), eq(PublicationVisibility.PUBLIC), eq(Set.of(PublicationType.DISH)), any()))
                .thenReturn(Page.empty());

        assertThat(publicationService.feed(Pageable.unpaged(), null, Set.of(PublicationType.DISH))).isEmpty();
        verify(publicationRepository, never()).findFeedForAuthenticatedViewerOrderByUnseenFirst(any(), any(), any(), any());
    }

    @Test
    void shouldFilterFeedByTypeForAuthenticatedUser() {
        UUID viewerId = UUID.randomUUID();
        Set<PublicationType> receitas = Set.of(PublicationType.RECIPE, PublicationType.MY_VERSION);
        when(publicationRepository.findFeedForAuthenticatedViewerOrderByUnseenFirst(
                eq(PublicationStatus.ACTIVE), eq(receitas), eq(viewerId), any()))
                .thenReturn(Page.empty());

        assertThat(publicationService.feed(Pageable.unpaged(), viewerId, receitas)).isEmpty();
    }

    @Test
    void shouldDefaultToAllTypesWhenNoneRequested() {
        when(publicationRepository.findByStatusAndVisibilityAndTypeInOrderByPublishedAtDescIdDesc(
                eq(PublicationStatus.ACTIVE), eq(PublicationVisibility.PUBLIC),
                eq(EnumSet.allOf(PublicationType.class)), any()))
                .thenReturn(Page.empty());

        assertThat(publicationService.feed(Pageable.unpaged(), null, Set.of())).isEmpty();
    }

    @Test
    void shouldRejectInvalidCreationAndMissingUpdateOrRemoval() {
        UUID id = UUID.randomUUID();
        CreatePublicationRequest invalid = new CreatePublicationRequest(
                UUID.randomUUID(), "RECIPE", "PUBLIC", null, null, "https://example.com/image.png", null, null);
        assertThatThrownBy(() -> publicationService.create(invalid))
                .isInstanceOf(org.application.service.exception.InvalidOperationException.class);

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> publicationService.update(id, new org.application.controller.publication.request.UpdatePublicationRequest(UUID.randomUUID(), null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> publicationService.remove(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> publicationService.createMyVersion(id, org.mockito.Mockito.mock(CreateMyVersionRequest.class)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateRecipeStepsAndTitleForMyVersion() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(PublicationType.MY_VERSION)
                .visibility(PublicationVisibility.PUBLIC)
                .title("Título antigo")
                .status(PublicationStatus.ACTIVE)
                .build();
        Recipe storedRecipe = Recipe.builder()
                .publicationId(id)
                .instructions("Passo antigo")
                .build();
        CreateRecipeRequest recipe = new CreateRecipeRequest(
                BigDecimal.valueOf(4),
                "porções",
                "Misture.\nAsse.",
                List.of(new CreateIngredientRequest((short) 1, "farinha", BigDecimal.ONE, "xícara", null))
        );

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(recipeRepository.findByPublicationIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(storedRecipe));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-10T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.update(id, new UpdatePublicationRequest(
                null, null, "Título corrigido", null, null, recipe
        ), authorId);

        assertThat(publication.getTitle()).isEqualTo("Título corrigido");
        assertThat(storedRecipe.getInstructions()).isEqualTo("Misture.\nAsse.");
        verify(recipeRepository).save(storedRecipe);
        verify(recipeIngredientRepository, times(2)).saveAll(any());
        var ingredientWriteOrder = inOrder(recipeIngredientRepository);
        ingredientWriteOrder.verify(recipeIngredientRepository).saveAll(any());
        ingredientWriteOrder.verify(recipeIngredientRepository).flush();
        ingredientWriteOrder.verify(recipeIngredientRepository).saveAll(any());
        verify(stringNormalizer, never()).normalize("Título corrigido");
    }

    @Test
    void shouldAllowAdminToEditPublicationOfAnotherAuthorAndMarkEditedByAdmin() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .title("Título antigo")
                .status(PublicationStatus.ACTIVE)
                .build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(adminId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(adminId).status(UserStatus.ACTIVE).role(UserRole.ADMIN).build()));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-11T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.update(id, new UpdatePublicationRequest(
                null, null, "Editado pelo admin", null, null, null
        ), adminId);

        assertThat(publication.getTitle()).isEqualTo("Editado pelo admin");
        assertThat(publication.getEditedByAdminId()).isEqualTo(adminId);
        assertThat(publication.getEditedByAdminAt()).isNotNull();
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("event=publication_edited_by_admin", "publicationId=" + id, "adminId=" + adminId, "authorId=" + authorId);
    }

    @Test
    void shouldAllowAdminToRemovePublicationOfAnotherAuthor() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .status(PublicationStatus.ACTIVE)
                .build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(adminId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(adminId).status(UserStatus.ACTIVE).role(UserRole.ADMIN).build()));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-11T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.remove(id, adminId);

        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.REMOVED);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("event=publication_removed_by_admin", "publicationId=" + id, "adminId=" + adminId, "authorId=" + authorId);
    }

    @Test
    void shouldRejectUpdateWhenActingUserIsNeitherAuthorNorAdmin() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .status(PublicationStatus.ACTIVE)
                .build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(strangerId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(strangerId).status(UserStatus.ACTIVE).role(UserRole.USER).build()));

        assertThatThrownBy(() -> publicationService.update(id, new UpdatePublicationRequest(
                null, null, "Tentativa indevida", null, null, null
        ), strangerId))
                .isInstanceOf(org.application.service.exception.InvalidOperationException.class)
                .hasMessageContaining("Somente o autor");
        assertThat(publication.getTitle()).isNull();
        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldRejectRemovalWhenActingUserIsNeitherAuthorNorAdmin() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .status(PublicationStatus.ACTIVE)
                .build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(strangerId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(strangerId).status(UserStatus.ACTIVE).role(UserRole.USER).build()));

        assertThatThrownBy(() -> publicationService.remove(id, strangerId))
                .isInstanceOf(org.application.service.exception.InvalidOperationException.class)
                .hasMessageContaining("Somente o autor");
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.ACTIVE);
        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldValidateImageBeforeCreatingUpload() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        Publication result = publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(saved);
        verify(imageValidatorClient).validate(content, "dish.png", "image/png");
        verify(imageStorage).store(processedContent, "dish.png", "image/webp");
        verify(publicationImageCheckRepository).save(any());
        // Sem EXIF, não deve haver um segundo save só para gravar photoTakenAt.
        verify(publicationRepository, times(1)).save(any(Publication.class));
    }

    @Test
    void shouldSkipNotificationSaveWhenAuthorHasNoFollowers() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectCreateWhenRateLimitExceeded() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        org.mockito.Mockito.doThrow(new RateLimitExceededException(
                        "Limite de publicações excedido. Tente novamente mais tarde.",
                        OffsetDateTime.parse("2026-08-09T12:10:00Z")))
                .when(publicationRateLimiter).recordAttempt(authorId);

        assertThatThrownBy(() -> publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(publicationRepository, never()).save(any());
    }

    @Test
    void shouldRecordAttemptWithAuthorIdOnEachCreation() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        verify(publicationRateLimiter).recordAttempt(authorId);
    }

    @Test
    void shouldResolveAndAttachTagsWhenCreatingPublication() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);
        org.application.model.Tag tag = org.application.model.Tag.builder()
                .id(UUID.randomUUID()).name("Vegano").slug("vegano").build();

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(tagService.resolveOrCreate(List.of("vegano"), authorId)).thenReturn(List.of(tag));

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null, List.of("vegano")),
                authorId, content, "dish.png", "image/png");

        verify(tagService).attachToPublication(saved.getId(), List.of(tag));
    }

    @Test
    void shouldReplaceTagsOnUpdateWhenTagsAreProvided() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id).authorId(authorId).type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC).status(PublicationStatus.ACTIVE)
                .build();
        org.application.model.Tag tag = org.application.model.Tag.builder()
                .id(UUID.randomUUID()).name("Doce").slug("doce").build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(tagService.resolveOrCreate(List.of("doce"), authorId)).thenReturn(List.of(tag));

        publicationService.update(id, new UpdatePublicationRequest(
                null, null, null, null, null, null, List.of("doce")
        ), authorId);

        verify(tagService).attachToPublication(id, List.of(tag));
    }

    @Test
    void shouldNotTouchTagsOnUpdateWhenTagsAreOmitted() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(id).authorId(authorId).type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC).status(PublicationStatus.ACTIVE)
                .build();

        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));

        publicationService.update(id, new UpdatePublicationRequest(
                null, null, "Só o título", null, null, null
        ), authorId);

        verify(tagService, never()).attachToPublication(any(), any());
        verify(tagService, never()).resolveOrCreate(any(), any());
    }

    @Test
    void shouldNotifyOnlyActiveFollowersWithPreferenceEnabledWhenPublishing() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = Publication.builder()
                .id(UUID.randomUUID())
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .status(PublicationStatus.ACTIVE)
                .build();
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, null);

        UUID eligibleFollowerId = UUID.randomUUID();
        UUID optedOutFollowerId = UUID.randomUUID();
        UUID inactiveFollowerId = UUID.randomUUID();

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(followRepository.findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(authorId), any()))
                .thenReturn(new PageImpl<>(List.of(
                        Follow.builder().followerId(eligibleFollowerId).followedId(authorId).build(),
                        Follow.builder().followerId(optedOutFollowerId).followedId(authorId).build(),
                        Follow.builder().followerId(inactiveFollowerId).followedId(authorId).build())));
        when(userRepository.findAllById(List.of(eligibleFollowerId, optedOutFollowerId, inactiveFollowerId)))
                .thenReturn(List.of(
                        User.builder().id(eligibleFollowerId).status(UserStatus.ACTIVE).notifyOnFollowedPublish(true).build(),
                        User.builder().id(optedOutFollowerId).status(UserStatus.ACTIVE).notifyOnFollowedPublish(false).build(),
                        User.builder().id(inactiveFollowerId).status(UserStatus.DELETED).notifyOnFollowedPublish(true).build()));

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<UserNotification>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<UserNotification> notifications = captor.getValue();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUserId()).isEqualTo(eligibleFollowerId);
        assertThat(notifications.get(0).getType()).isEqualTo("FOLLOWED_USER_PUBLISHED");
        assertThat(notifications.get(0).getActorId()).isEqualTo(authorId);
        assertThat(notifications.get(0).getPublicationId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldRecordPhotoTakenAtWhenValidatorReturnsExifDateTime() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, "2026-08-15T14:32:07");

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        assertThat(saved.getPhotoTakenAt()).isEqualTo(OffsetDateTime.parse("2026-08-15T14:32:07Z"));
        // Um save para criar a publicação, outro para gravar o photoTakenAt lido do EXIF.
        verify(publicationRepository, times(2)).save(any(Publication.class));
    }

    @Test
    void shouldIgnoreMalformedPhotoTakenAtFromValidator() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        byte[] processedContent = {4, 5, 6, 7};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                processedContent, "image/webp", 800, 600, 0.98, 0.75, "not-a-valid-datetime");

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(processedContent, "dish.png", "image/webp"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        assertThat(saved.getPhotoTakenAt()).isNull();
        verify(publicationRepository, times(1)).save(any(Publication.class));
    }

    @Test
    void shouldNotStoreImageWhenValidatorRejectsIt() {
        byte[] content = {1, 2, 3};
        org.application.service.exception.InvalidOperationException rejection =
                new org.application.service.exception.InvalidOperationException("IMAGE_NOT_FOOD", "A imagem precisa apresentar uma comida.");
        when(imageValidatorClient.validate(content, "person.png", "image/png")).thenThrow(rejection);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publicationService.createUpload(
                        new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                        UUID.randomUUID(), content, "person.png", "image/png"))
                .isSameAs(rejection);

        verify(imageStorage, never()).store(any(byte[].class), any(), any());
        verify(publicationRepository, never()).save(any(Publication.class));
    }

    @Test
    void shouldNotStoreImageWhenValidatorIsUnavailable() {
        byte[] content = {1, 2, 3};
        org.application.service.exception.InvalidOperationException unavailable =
                new org.application.service.exception.InvalidOperationException("IMAGE_VALIDATOR_UNAVAILABLE", "O validador de imagens está indisponível.");
        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenThrow(unavailable);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publicationService.createUpload(
                        new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                        UUID.randomUUID(), content, "dish.png", "image/png"))
                .isSameAs(unavailable);

        verify(imageStorage, never()).store(any(byte[].class), any(), any());
        verify(publicationRepository, never()).save(any(Publication.class));
    }

    @Test
    void shouldHidePrivatePublicationFromNonAuthor() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Publication publication = privatePublication(id, authorId);
        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));

        assertThatThrownBy(() -> publicationService.findAccessible(id, strangerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publicação não encontrada.");
    }

    @Test
    void shouldAllowPrivatePublicationForAuthor() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Publication publication = privatePublication(id, authorId);
        when(publicationRepository.findByIdAndStatus(id, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));

        assertThat(publicationService.findAccessible(id, authorId)).isSameAs(publication);
    }

    @Test
    void shouldHidePrivatePublicationFromAnonymousVisitor() {
        UUID id = UUID.randomUUID();
        when(publicationRepository.findByIdAndStatusAndVisibility(id, PublicationStatus.ACTIVE, PublicationVisibility.PUBLIC))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.findAccessible(id, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldExcludePrivatePublicationsFromStrangersProfile() {
        UUID authorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        when(publicationRepository.findByAuthorIdAndStatusAndVisibilityInOrderByPublishedAtDescIdDesc(
                eq(authorId), eq(PublicationStatus.ACTIVE), any(), any())).thenReturn(Page.empty());

        publicationService.profile(authorId, Pageable.unpaged(), viewerId);

        verify(publicationRepository).findByAuthorIdAndStatusAndVisibilityInOrderByPublishedAtDescIdDesc(
                eq(authorId), eq(PublicationStatus.ACTIVE),
                eq(java.util.EnumSet.of(PublicationVisibility.PUBLIC, PublicationVisibility.INTERNAL)), any());
        verify(publicationRepository, never()).findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(any(), any(), any());
    }

    @Test
    void shouldShowAllVisibilitiesOnOwnProfile() {
        UUID authorId = UUID.randomUUID();
        when(publicationRepository.findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(authorId, PublicationStatus.ACTIVE, Pageable.unpaged()))
                .thenReturn(Page.empty());

        publicationService.profile(authorId, Pageable.unpaged(), authorId);

        verify(publicationRepository).findByAuthorIdAndStatusOrderByPublishedAtDescIdDesc(authorId, PublicationStatus.ACTIVE, Pageable.unpaged());
    }

    @Test
    void shouldNotNotifyFollowersWhenPublicationIsPrivate() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Publication publication = privatePublication(id, authorId);

        ReflectionTestUtils.invokeMethod(publicationService, "notifyFollowers", publication);

        verify(followRepository, never()).findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any());
    }

    private Publication privatePublication(UUID id, UUID authorId) {
        return Publication.builder()
                .id(id)
                .authorId(authorId)
                .type(org.application.model.PublicationType.DISH)
                .visibility(PublicationVisibility.PRIVATE)
                .status(PublicationStatus.ACTIVE)
                .build();
    }

    private Publication publication(UUID id) {
        return Publication.builder()
                .id(id)
                .authorId(UUID.randomUUID())
                .type(org.application.model.PublicationType.DISH)
                .visibility(org.application.model.PublicationVisibility.PUBLIC)
                .gcsBucket("comesebebes-dev-images")
                .gcsObjectName("images/" + id + ".webp")
                .gcsGeneration(1L)
                .imageFormat("webp")
                .imageSizeBytes(1024L)
                .imageWidth(800)
                .imageHeight(600)
                .status(PublicationStatus.ACTIVE)
                .build();
    }
}
