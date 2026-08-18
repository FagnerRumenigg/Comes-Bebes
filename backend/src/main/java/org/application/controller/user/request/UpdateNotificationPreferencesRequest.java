package org.application.controller.user.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateNotificationPreferencesRequest", description = "Preferências de notificação da conta autenticada.")
public record UpdateNotificationPreferencesRequest(
        @Schema(description = "Notificar quando alguém que você segue publicar.", example = "true")
        boolean notifyOnFollowedPublish
) {
}
