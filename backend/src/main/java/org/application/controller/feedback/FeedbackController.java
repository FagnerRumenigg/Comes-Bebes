package org.application.controller.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.feedback.request.CreateFeedbackRequest;
import org.application.controller.feedback.response.FeedbackResponse;
import org.application.dto.PageResponse;
import org.application.service.FeedbackResponseFactory;
import org.application.service.FeedbackService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Feedback", description = "\"Falar com a gente\" — sugestões e problemas relatados pelos usuários.")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackResponseFactory feedbackResponseFactory;
    private final CurrentUser currentUser;
    private final ZoneId applicationZoneId;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Enviar sugestão", description = "Registra uma sugestão ou problema relatado pelo usuário autenticado.")
    @ApiResponse(responseCode = "204", description = "Sugestão registrada.")
    public ResponseEntity<Void> submit(@Valid @RequestBody CreateFeedbackRequest request, Authentication authentication) {
        feedbackService.submit(currentUser.id(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "listFeedback", summary = "Listar mensagens", description = "Retorna as mensagens de \"Falar com a gente\" paginadas, mais recente primeiro. Só quem administra o app pode ler.")
    @Parameters({
            @Parameter(name = "page", description = "Número da página, iniciando em 1.", schema = @Schema(type = "integer", minimum = "1", example = "1")),
            @Parameter(name = "size", description = "Quantidade de itens por página.", schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "20"))
    })
    @ApiResponse(responseCode = "200", description = "Mensagens retornadas.")
    public PageResponse<FeedbackResponse> list(@Parameter(hidden = true) Pageable pageable) {
        return feedbackResponseFactory.of(feedbackService.list(pageable), applicationZoneId);
    }
}
