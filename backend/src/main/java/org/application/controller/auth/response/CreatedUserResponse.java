package org.application.controller.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.application.model.User;
import org.application.model.UserRole;

import java.util.UUID;

@Schema(name = "CreatedUserResponse", description = "Dados retornados após a criação da conta.")
public record CreatedUserResponse(
        @Schema(description = "Identificador único do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
        UUID id,
        @Schema(description = "E-mail da conta — é a credencial de login.", example = "fagner@exemplo.com.br")
        String email,
        @Schema(description = "@usuário, gerado automaticamente a partir do nome de exibição.", example = "fagner")
        String username,
        @Schema(description = "Nome exibido no perfil.", example = "Fagner")
        String displayName,
        @Schema(description = "Papel de acesso da conta.", example = "USER")
        UserRole role
) {
    public static CreatedUserResponse of(User user) {
        return new CreatedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole());
    }
}
