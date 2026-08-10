package org.application.controller.user.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateUserRequest", description = "Campos opcionais para atualizar o perfil do usuário.")
public record UpdateUserRequest(
        @Schema(description = "Novo nome público único.", example = "fagner_cozinha", nullable = true)
        @Pattern(regexp = "[a-zA-Z0-9_]{3,30}") String username,
        @Schema(description = "Novo nome exibido no perfil.", example = "Fagner da Cozinha", nullable = true)
        @Size(max = 100) String displayName
) {
}
