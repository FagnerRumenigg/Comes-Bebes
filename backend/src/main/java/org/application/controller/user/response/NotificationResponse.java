package org.application.controller.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.UserNotification;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "NotificationResponse", description = "Notificação privada do usuário (docs/telas/12-avisos.html).")
public record NotificationResponse(
        UUID id,
        String type,
        UUID moderationCaseId,
        UUID publicationId,
        @Schema(description = "Coleção relacionada; preenchido para avisos de coleção.", nullable = true)
        UUID collectionId,
        @Schema(description = "Usuário que originou a notificação (ex.: quem passou a seguir). Nulo quando não se aplica.", nullable = true)
        UUID actorId,
        @Schema(description = "Nome de exibição de quem originou a notificação.", nullable = true)
        String actorDisplayName,
        @Schema(description = "Título da publicação relacionada, quando houver.", nullable = true)
        String publicationTitle,
        @Schema(description = "Imagem da publicação relacionada, quando houver.", nullable = true)
        String publicationImageUrl,
        @Schema(description = "Nome da coleção relacionada, quando houver.", nullable = true)
        String collectionName,
        OffsetDateTime createdAt,
        @Schema(description = "Momento da leitura; nulo enquanto não lida.", nullable = true)
        OffsetDateTime readAt
) {
    public static NotificationResponse of(UserNotification item) {
        return builder().id(item.getId()).type(item.getType()).moderationCaseId(item.getModerationCaseId())
                .publicationId(item.getPublicationId()).collectionId(item.getCollectionId())
                .actorId(item.getActorId()).createdAt(item.getCreatedAt()).readAt(item.getReadAt()).build();
    }
}
