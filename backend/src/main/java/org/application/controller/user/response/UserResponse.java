package org.application.controller.user.response;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.application.model.User;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import java.util.UUID;
import java.time.ZoneId;

@Builder
@Schema(name = "UserResponse", description = "Representação pública dos dados de um usuário.")
public record UserResponse(
        @Schema(description = "Identificador único do usuário.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
        UUID id,
        @Schema(description = "Nome público do usuário.", example = "fagner")
        String username,
        @Schema(description = "Nome exibido no perfil.", example = "Fagner")
        String displayName,
        @Schema(description = "Papel de acesso da conta.", example = "USER")
        UserRole role,
        @Schema(description = "Estado atual da conta.", example = "ACTIVE")
        UserStatus status,
        @Schema(description = "Indica se os totais das reações ficam visíveis.", example = "true")
        boolean showReactionCounts
) {

    public static UserResponse of(User user, ZoneId zoneId) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .status(user.getStatus())
                .showReactionCounts(user.isShowReactionCounts())
                .build();
    }
}

