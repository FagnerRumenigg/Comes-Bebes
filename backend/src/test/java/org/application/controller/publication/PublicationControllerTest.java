package org.application.controller.publication;

import org.application.controller.publication.response.PublicationResponse;
import org.application.controller.ApiExceptionHandler;
import org.application.controller.response.ApiErrorResponse;
import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.service.PublicationService;
import org.application.service.RecipeService;
import org.application.service.ReactionService;
import org.application.service.SavedPublicationService;
import org.application.service.ReportService;
import org.application.service.PublicationResponseFactory;
import org.application.service.storage.ImageStorage;
import org.application.config.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicationControllerTest {

    @Mock
    private PublicationService publicationService;

    @Mock private RecipeService recipeService;
    @Mock private ReactionService reactionService;
    @Mock private SavedPublicationService savedPublicationService;
    @Mock private ReportService reportService;
    @Mock private PublicationResponseFactory responseFactory;
    @Mock private ImageStorage imageStorage;

    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicationController(
                        publicationService,
                        recipeService,
                        reactionService,
                        savedPublicationService,
                        reportService,
                        responseFactory,
                        imageStorage,
                        ZoneId.of("America/Sao_Paulo"),
                        currentUser
                ))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        org.mockito.Mockito.lenient().when(currentUser.id(any(), any(UUID.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        org.mockito.Mockito.lenient().when(currentUser.id(any()))
                .thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(responseFactory.of(any(Publication.class), any(ZoneId.class), nullable(UUID.class)))
                .thenAnswer(invocation -> PublicationResponse.builder()
                        .id(invocation.<Publication>getArgument(0).getId())
                        .type(PublicationType.DISH)
                        .visibility(PublicationVisibility.PUBLIC)
                        .status(PublicationStatus.ACTIVE)
                        .build());
    }

    @Test
    void shouldFindPublication() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicationService.findAccessible(id, null)).thenReturn(publication(id));

        mockMvc.perform(get("/publications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("DISH"));
    }

    @Test
    void shouldRemovePublication() throws Exception {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        mockMvc.perform(delete("/publications/{id}", id)
                        .contentType("application/json")
                        .content("{\"authorId\":\"" + authorId + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListFeedAndSearch() throws Exception {
        when(publicationService.feed(any(org.springframework.data.domain.Pageable.class), nullable(UUID.class), nullable(java.util.Set.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(publicationService.search(any(), any(), any(org.springframework.data.domain.Pageable.class), nullable(UUID.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/publications/feed")).andExpect(status().isOk());
        mockMvc.perform(get("/publications/search?title=lasanha")).andExpect(status().isOk());
    }

    @Test
    void shouldFilterFeedByTypes() throws Exception {
        when(publicationService.feed(any(org.springframework.data.domain.Pageable.class), nullable(UUID.class),
                eq(java.util.Set.of(PublicationType.DISH))))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/publications/feed?types=DISH")).andExpect(status().isOk());
    }

    @Test
    void shouldListSavedPublications() throws Exception {
        when(savedPublicationService.list(any(UUID.class), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/publications/saved/{userId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldApplyReactionAndSavePublication() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String reaction = "{\"userId\":\"" + userId + "\",\"reactionCode\":\"WOULD_EAT\"}";
        String saved = "{\"userId\":\"" + userId + "\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/publications/{id}/reactions", id)
                        .contentType("application/json").content(reaction))
                .andExpect(status().isNoContent());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/publications/{id}/saved", id)
                        .contentType("application/json").content(saved))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRemoveReactionAndSavedPublication() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String reaction = "{\"userId\":\"" + userId + "\",\"reactionCode\":\"WOULD_EAT\"}";
        String saved = "{\"userId\":\"" + userId + "\"}";

        mockMvc.perform(delete("/publications/{id}/reactions", id).contentType("application/json").content(reaction))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/publications/{id}/saved", id).contentType("application/json").content(saved))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReportPublication() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/publications/{id}/reports", id)
                        .contentType("application/json")
                        .content("{\"reporterId\":\"" + userId + "\",\"reasonCode\":\"NOT_FOOD\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCreateAndUpdatePublication() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicationService.create(any(org.application.controller.publication.request.CreatePublicationRequest.class), any(UUID.class)))
                .thenReturn(publication(id));
        when(publicationService.update(any(UUID.class), any(org.application.controller.publication.request.UpdatePublicationRequest.class), any(UUID.class)))
                .thenReturn(publication(id));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/publications")
                        .contentType("application/json")
                        .content("{\"authorId\":\"" + UUID.randomUUID() + "\",\"type\":\"DISH\",\"visibility\":\"PUBLIC\",\"imageUrl\":\"https://example.com/dish.png\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/publications/{id}", id)
                        .contentType("application/json")
                        .content("{\"authorId\":\"" + UUID.randomUUID() + "\",\"title\":\"Novo título\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFindRecipe() throws Exception {
        UUID id = UUID.randomUUID();
        when(recipeService.findDetails(id)).thenReturn(new RecipeService.RecipeDetails(
                org.application.model.Recipe.builder().publicationId(id).instructions("Preparar").build(), List.of()));

        mockMvc.perform(get("/publications/{id}/recipe", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationId").value(id.toString()));
    }

    @Test
    void shouldCreateMyVersion() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicationService.createMyVersionUpload(any(UUID.class), any(org.application.controller.publication.request.CreateMyVersionRequest.class), any(UUID.class), any(byte[].class), any(String.class), any(String.class)))
                .thenReturn(publication(id));
        String body = "{\"authorId\":\"" + UUID.randomUUID() + "\",\"visibility\":\"PUBLIC\",\"titleSuffix\":\"com legumes\",\"changeSummary\":\"Alterei\",\"recipe\":{\"instructions\":\"Preparar\",\"ingredients\":[{\"position\":1,\"name\":\"Tomate\"}]}}";
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", body.getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "version.webp", "image/webp", new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/publications/{id}/my-versions", id)
                        .file(data).file(image))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCreatePublicationWithMultipartImage() throws Exception {
        UUID id = UUID.randomUUID();
        when(publicationService.createUpload(any(org.application.controller.publication.request.CreatePublicationUploadRequest.class), any(UUID.class), any(byte[].class), any(String.class), any(String.class)))
                .thenReturn(publication(id));
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json",
                "{\"type\":\"DISH\",\"visibility\":\"PUBLIC\"}".getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "dish.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/publications")
                        .file(data).file(image))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestWhenImageIsNotFood() throws Exception {
        when(publicationService.createUpload(any(org.application.controller.publication.request.CreatePublicationUploadRequest.class), any(UUID.class), any(byte[].class), any(String.class), any(String.class)))
                .thenThrow(new org.application.service.exception.InvalidOperationException(
                        "IMAGE_NOT_FOOD", "A imagem precisa apresentar uma comida."));
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json",
                "{\"type\":\"DISH\",\"visibility\":\"PUBLIC\"}".getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "person.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/publications")
                        .file(data).file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOOD"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenImageValidatorIsDown() throws Exception {
        when(publicationService.createUpload(any(org.application.controller.publication.request.CreatePublicationUploadRequest.class), any(UUID.class), any(byte[].class), any(String.class), any(String.class)))
                .thenThrow(new org.application.service.exception.InvalidOperationException(
                        "IMAGE_VALIDATOR_UNAVAILABLE", "O validador de imagens está indisponível."));
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json",
                "{\"type\":\"DISH\",\"visibility\":\"PUBLIC\"}".getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "dish.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/publications")
                        .file(data).file(image))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("IMAGE_VALIDATOR_UNAVAILABLE"));
    }

    private Publication publication(UUID id) {
        return Publication.builder()
                .id(id)
                .authorId(UUID.randomUUID())
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
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
