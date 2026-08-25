package org.application.controller.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciais para autenticação.")
public record LoginRequest(
        @Schema(description = "E-mail ou @usuário da conta. Contas sem e-mail cadastrado (criadas antes da "
                + "migração — produto5.md v5 §5.1) só aceitam @usuário; assim que o e-mail é definido, o "
                + "login passa a exigir e-mail.", example = "fagner@exemplo.com.br")
        @NotBlank String identifier,
        @Schema(example = "MinhaSenha123!") @NotBlank String password
) {
}
