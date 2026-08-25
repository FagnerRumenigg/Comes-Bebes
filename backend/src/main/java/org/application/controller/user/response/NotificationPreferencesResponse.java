package org.application.controller.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.User;

@Builder
@Schema(name = "NotificationPreferencesResponse", description = "Preferências de aviso da conta autenticada (docs/telas/09-configuracoes.html, seção \"Avisos\").")
public record NotificationPreferencesResponse(
        @Schema(description = "Quando alguém que você segue publica.", example = "false")
        boolean notifyOnFollowedPublish,
        @Schema(description = "Quando alguém guarda uma publicação sua.", example = "true")
        boolean notifyOnSaved,
        @Schema(description = "Quando alguém reage a uma publicação sua.", example = "true")
        boolean notifyOnReacted,
        @Schema(description = "Quando alguém faz a própria versão de uma receita sua.", example = "true")
        boolean notifyOnMyVersion,
        @Schema(description = "Quando entra coisa nova numa coleção que você segue.", example = "true")
        boolean notifyOnCollectionNewItem,
        @Schema(description = "Quando alguém compartilha uma coleção com você.", example = "true")
        boolean notifyOnCollectionShared,
        @Schema(description = "Receber um resumo semanal por e-mail.", example = "false")
        boolean notifyWeeklyEmail
) {
    public static NotificationPreferencesResponse of(User user) {
        return NotificationPreferencesResponse.builder()
                .notifyOnFollowedPublish(user.isNotifyOnFollowedPublish())
                .notifyOnSaved(user.isNotifyOnSaved())
                .notifyOnReacted(user.isNotifyOnReacted())
                .notifyOnMyVersion(user.isNotifyOnMyVersion())
                .notifyOnCollectionNewItem(user.isNotifyOnCollectionNewItem())
                .notifyOnCollectionShared(user.isNotifyOnCollectionShared())
                .notifyWeeklyEmail(user.isNotifyWeeklyEmail())
                .build();
    }
}
