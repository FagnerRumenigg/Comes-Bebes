package org.application.controller.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest", description = "Senha atual e nova senha da conta autenticada.")
public record ChangePasswordRequest(
        @NotBlank @Schema(description = "Senha atual.", format = "password") String currentPassword,
        @NotBlank @Size(min = 8, max = 72) @Schema(description = "Nova senha.", format = "password") String newPassword
) {
}
