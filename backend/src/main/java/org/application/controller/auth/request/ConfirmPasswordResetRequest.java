package org.application.controller.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ConfirmPasswordResetRequest", description = "Confirmação de troca de senha usando o token recebido por e-mail.")
public record ConfirmPasswordResetRequest(
        @Schema(description = "Token do link recebido por e-mail.")
        @NotBlank String token,
        @Schema(description = "Nova senha.", format = "password")
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
