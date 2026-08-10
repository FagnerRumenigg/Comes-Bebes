package org.application.controller.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.UserNotification;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "NotificationResponse", description = "Notificação privada do usuário.")
public record NotificationResponse(
        UUID id,
        String type,
        UUID moderationCaseId,
        UUID publicationId,
        @Schema(description = "Momento da leitura; nulo enquanto não lida.", nullable = true)
        OffsetDateTime readAt
) {
    public static NotificationResponse of(UserNotification item) {
        return builder().id(item.getId()).type(item.getType()).moderationCaseId(item.getModerationCaseId())
                .publicationId(item.getPublicationId()).readAt(item.getReadAt()).build();
    }
}
