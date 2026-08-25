package org.application.controller.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.application.model.PublicationVisibility;
import org.application.model.User;

@Schema(name = "UserInfoResponse", description = "Informações básicas da conta autenticada que não vêm no token de login (docs/telas/04 — o menu da conta mostra o nome de exibição, não o @usuário).")
public record UserInfoResponse(
        @Schema(description = "Nome exibido no perfil.", example = "Fagner")
        String displayName,
        @Schema(description = "Visibilidade padrão para novas publicações (docs/telas/09-configuracoes.html). Preferência privada — não faz parte do UserResponse público.", example = "PUBLIC")
        PublicationVisibility defaultPublicationVisibility
) {
    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(user.getDisplayName(), user.getDefaultPublicationVisibility());
    }
}
