package org.application.controller.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.User;

@Builder
@Schema(name = "NotificationPreferencesResponse", description = "Preferências de notificação da conta autenticada.")
public record NotificationPreferencesResponse(
        @Schema(description = "Notificar quando alguém que você segue publicar.", example = "true")
        boolean notifyOnFollowedPublish
) {
    public static NotificationPreferencesResponse of(User user) {
        return new NotificationPreferencesResponse(user.isNotifyOnFollowedPublish());
    }
}
