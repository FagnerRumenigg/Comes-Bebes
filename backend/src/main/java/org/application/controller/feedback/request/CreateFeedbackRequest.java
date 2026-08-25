package org.application.controller.feedback.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateFeedbackRequest", description = "\"Falar com a gente\" (docs/telas/09-configuracoes.html).")
public record CreateFeedbackRequest(
        @Schema(example = "Achei o app muito bom, mas seria legal ter busca por tipo de prato.")
        @NotBlank @Size(max = 4000) String message,
        @Schema(description = "Opcional — pra responder num e-mail diferente do cadastrado.", nullable = true, example = "fagner@exemplo.com.br")
        @Email @Size(max = 320) String contactEmail
) {
}
