package org.application.controller.feedback.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "FeedbackResponse", description = "Mensagem enviada em \"Falar com a gente\", pra leitura administrativa.")
public record FeedbackResponse(
        UUID id,
        String message,
        @Schema(description = "E-mail informado pra resposta, quando a pessoa preencheu.", nullable = true)
        String contactEmail,
        @Schema(description = "Quem enviou.", nullable = true)
        UUID userId,
        @Schema(description = "Nome de exibição de quem enviou.", nullable = true)
        String userDisplayName,
        @Schema(description = "@usuário de quem enviou.", nullable = true)
        String username,
        OffsetDateTime createdAt
) {
}
