package org.application.controller.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciais para autenticação.")
public record LoginRequest(
        @Schema(example = "fagner") @NotBlank String username,
        @Schema(example = "MinhaSenha123!") @NotBlank String password
) {
}
