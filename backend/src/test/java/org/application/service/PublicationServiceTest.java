package org.application.service;

import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.model.Recipe;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.repository.PublicationRepository;
import org.application.repository.PublicationImageCheckRepository;
import org.application.repository.RecipeRepository;
import org.application.repository.RecipeIngredientRepository;
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
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

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

    @InjectMocks
    private PublicationService publicationService;

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
        when(publicationRepository.findByStatusAndVisibilityOrderByPublishedAtDescIdDesc(any(), any(), any()))
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
    void shouldRejectInvalidCreationAndMissingUpdateOrRemoval() {
        UUID id = UUID.randomUUID();
        CreatePublicationRequest invalid = new CreatePublicationRequest(
                UUID.randomUUID(), "RECIPE", "PUBLIC", null, null, "https://example.com/image.png", null);
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
        when(stringNormalizer.normalize("Título corrigido")).thenReturn("Título corrigido");
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
    }

    @Test
    void shouldValidateImageBeforeCreatingUpload() {
        UUID authorId = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        Publication saved = publication(UUID.randomUUID());
        ImageValidatorClient.ValidationResult validation = new ImageValidatorClient.ValidationResult(
                "approved", new ImageValidatorClient.Classification("FOOD", 0.98, 0.75));

        when(imageValidatorClient.validate(content, "dish.png", "image/png")).thenReturn(validation);
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).status(UserStatus.ACTIVE).build()));
        when(imageStorage.store(content, "dish.png", "image/png"))
                .thenReturn(StoredImage.builder().bucket("bucket").objectName("dish.png").build());
        when(publicationRepository.save(any(Publication.class))).thenReturn(saved);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-09T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        Publication result = publicationService.createUpload(
                new CreatePublicationUploadRequest("DISH", "PUBLIC", null, null, null),
                authorId, content, "dish.png", "image/png");

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(saved);
        verify(imageValidatorClient).validate(content, "dish.png", "image/png");
        verify(imageStorage).store(content, "dish.png", "image/png");
        verify(publicationImageCheckRepository).save(any());
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
