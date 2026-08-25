package org.application.controller.collection.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "InviteCollectionMemberRequest", description = "Convite direto por @usuário para uma coleção \"Para quem eu escolher\".")
public record InviteCollectionMemberRequest(
        @Schema(description = "@usuário de quem convidar.", example = "maria")
        @NotBlank String username
) {
}
