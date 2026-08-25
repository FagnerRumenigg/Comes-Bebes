package org.application.controller.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.application.config.CurrentUser;
import org.application.controller.feedback.request.CreateFeedbackRequest;
import org.application.service.FeedbackService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Feedback", description = "\"Falar com a gente\" — sugestões e problemas relatados pelos usuários.")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Enviar sugestão", description = "Registra uma sugestão ou problema relatado pelo usuário autenticado.")
    @ApiResponse(responseCode = "204", description = "Sugestão registrada.")
    public ResponseEntity<Void> submit(@Valid @RequestBody CreateFeedbackRequest request, Authentication authentication) {
        feedbackService.submit(currentUser.id(authentication), request);
        return ResponseEntity.noContent().build();
    }
}
