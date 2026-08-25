package org.application.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.application.config.CurrentUser;
import org.application.controller.publication.PublicationController;
import org.application.controller.publication.response.PublicationResponse;
import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.service.PublicationResponseFactory;
import org.application.service.PublicationService;
import org.application.service.ReactionService;
import org.application.service.RecipeService;
import org.application.service.ReportService;
import org.application.service.SavedPublicationService;
import org.application.service.PublicationViewService;
import org.application.service.storage.ImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock private PublicationService publicationService;
    @Mock private RecipeService recipeService;
    @Mock private ReactionService reactionService;
    @Mock private SavedPublicationService savedPublicationService;
    @Mock private PublicationViewService publicationViewService;
    @Mock private ReportService reportService;
    @Mock private org.application.service.PhotoValidationFeedbackService photoValidationFeedbackService;
    @Mock private PublicationResponseFactory responseFactory;
    @Mock private ImageStorage imageStorage;
    @Mock private CurrentUser currentUser;

    private MockMvc mockMvc;
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicationController(
                        publicationService,
                        recipeService,
                        reactionService,
                        savedPublicationService,
                        publicationViewService,
                        reportService,
                        photoValidationFeedbackService,
                        responseFactory,
                        imageStorage,
                        ZoneId.of("America/Sao_Paulo"),
                        currentUser
                ))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        handlerLogger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        handlerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(appender);
    }

    @Test
    void shouldReturnStructuredResponseAndLogUnexpectedException() throws Exception {
        when(publicationService.findAccessible(any(UUID.class), nullable(UUID.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/publications/{id}", UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado."));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("event=unhandled_exception");
        assertThat(appender.list.get(0).getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Test
    void shouldReturnBadRequestForMalformedPathVariable() throws Exception {
        mockMvc.perform(get("/publications/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldReturnMethodNotAllowedInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/publications/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldReturnTooManyRequestsWithNextAvailableAt() throws Exception {
        java.time.OffsetDateTime nextAvailableAt = java.time.OffsetDateTime.parse("2026-08-09T12:10:00Z");
        when(publicationService.findAccessible(any(UUID.class), nullable(UUID.class)))
                .thenThrow(new org.application.service.exception.RateLimitExceededException(
                        "Limite de publicações excedido. Tente novamente mais tarde.", nextAvailableAt));

        mockMvc.perform(get("/publications/{id}", UUID.randomUUID()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.nextAvailableAt").value("2026-08-09T12:10:00Z"));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldNotInterfereWithExistingResourceNotFoundHandling() throws Exception {
        when(publicationService.findAccessible(any(UUID.class), nullable(UUID.class)))
                .thenThrow(new org.application.service.exception.ResourceNotFoundException(
                        "PUBLICATION_NOT_FOUND", "Publicação não encontrada."));

        mockMvc.perform(get("/publications/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUBLICATION_NOT_FOUND"));

        assertThat(appender.list).isEmpty();
    }
}
