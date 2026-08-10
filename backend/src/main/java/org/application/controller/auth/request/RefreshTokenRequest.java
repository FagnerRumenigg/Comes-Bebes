package org.application.controller.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshTokenRequest", description = "Refresh token usado para emitir uma nova sessão.")
public record RefreshTokenRequest(
        @NotBlank @Schema(description = "Refresh token retornado no login.") String refreshToken
) {
}
