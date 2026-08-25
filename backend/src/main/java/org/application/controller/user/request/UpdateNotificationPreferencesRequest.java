package org.application.controller.user.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateNotificationPreferencesRequest", description = "Preferências de aviso opcionais — campo nulo significa \"não mexer\" (docs/telas/09-configuracoes.html, seção \"Avisos\").")
public record UpdateNotificationPreferencesRequest(
        @Schema(description = "Quando alguém que você segue publica.", nullable = true)
        Boolean notifyOnFollowedPublish,
        @Schema(description = "Quando alguém guarda uma publicação sua.", nullable = true)
        Boolean notifyOnSaved,
        @Schema(description = "Quando alguém reage a uma publicação sua.", nullable = true)
        Boolean notifyOnReacted,
        @Schema(description = "Quando alguém faz a própria versão de uma receita sua.", nullable = true)
        Boolean notifyOnMyVersion,
        @Schema(description = "Quando entra coisa nova numa coleção que você segue.", nullable = true)
        Boolean notifyOnCollectionNewItem,
        @Schema(description = "Quando alguém compartilha uma coleção com você.", nullable = true)
        Boolean notifyOnCollectionShared,
        @Schema(description = "Receber um resumo semanal por e-mail.", nullable = true)
        Boolean notifyWeeklyEmail
) {
}
