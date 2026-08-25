package org.application.controller.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateUserRequest", description = "Dados necessários para criar uma conta.")
public record CreateUserRequest(
        @Schema(description = "E-mail da conta — é a credencial de login (produto5.md v5 §5.1).", example = "fagner@exemplo.com.br")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Senha da conta. Deve possuir entre 8 e 72 caracteres.", example = "MinhaSenha123!", format = "password")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "Nome exibido no perfil. O @usuário é gerado automaticamente a partir dele (impl10.md v10 §19.4), editável depois em Configurações.", example = "Fagner")
        @NotBlank @Size(max = 100) String displayName
) {
}
