package org.application.controller.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RequestPasswordResetRequest", description = "Pedido de link para trocar a senha (docs/telas/11-recuperar-senha.html).")
public record RequestPasswordResetRequest(
        @Schema(description = "E-mail da conta.", example = "fagner@exemplo.com.br")
        @NotBlank @Email String email
) {
}
