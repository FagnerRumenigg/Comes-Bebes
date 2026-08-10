package org.application.controller.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateUserRequest", description = "Dados necessários para criar uma conta.")
public record CreateUserRequest(
        @Schema(description = "Senha da conta. Deve possuir entre 8 e 72 caracteres.", example = "MinhaSenha123!", format = "password")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "Nome público único, usando letras, números e underscore.", example = "fagner")
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,30}") String username,
        @Schema(description = "Nome exibido no perfil.", example = "Fagner")
        @NotBlank @Size(max = 100) String displayName
) {
}
