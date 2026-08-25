package org.application.controller.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateUserRequest", description = "Campos opcionais para atualizar o perfil do usuário.")
public record UpdateUserRequest(
        @Schema(description = "Texto de origem do novo @usuário. Passa pela mesma normalização e "
                + "resolução de colisão da criação de conta (impl10.md v10 §19.4): minúsculas, sem "
                + "acento, espaço vira underscore, caracteres inválidos somem, e um sufixo numérico "
                + "é acrescentado automaticamente se o resultado colidir com alguém.",
                example = "Fagner Cozinha", nullable = true)
        @Size(min = 1, max = 100) String username,
        @Schema(description = "Novo nome exibido no perfil.", example = "Fagner da Cozinha", nullable = true)
        @Size(max = 100) String displayName,
        @Schema(description = "Nova descrição do perfil. String vazia limpa a descrição.",
                example = "Cozinho mais no fim de semana.", nullable = true)
        @Size(max = 280) String bio,
        @Schema(description = "Define o e-mail da conta. Contas antigas sem e-mail (produto5.md v5 §5.1) "
                + "usam este campo para migrar — depois de definido, o login passa a exigir e-mail.",
                example = "fagner@exemplo.com.br", nullable = true)
        @Email @Size(max = 254) String email,
        @Schema(description = "Nova visibilidade padrão para as próximas publicações "
                + "(docs/telas/09-configuracoes.html). Vale só dali pra frente — publicações já "
                + "existentes não mudam.", example = "PUBLIC", nullable = true)
        @Pattern(regexp = "PUBLIC|INTERNAL|PRIVATE") String defaultPublicationVisibility
) {
}
