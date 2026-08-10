package org.application.controller.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.application.model.User;
import org.application.model.UserRole;

import java.util.UUID;

@Schema(name = "CreatedUserResponse", description = "Dados retornados após a criação da conta.")
public record CreatedUserResponse(
        @Schema(description = "Identificador único do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
        UUID id,
        @Schema(description = "Nome público do usuário.", example = "fagner")
        String username,
        @Schema(description = "Nome exibido no perfil.", example = "Fagner")
        String displayName,
        @Schema(description = "Papel de acesso da conta.", example = "USER")
        UserRole role,
        @Schema(description = "Indica se os totais das reações ficam visíveis.", example = "true")
        boolean showReactionCounts
) {
    public static CreatedUserResponse of(User user) {
        return new CreatedUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isShowReactionCounts());
    }
}

