package org.application.controller.collection.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CollectionInviteResponse", description = "Token do link de convite ativo da coleção.")
public record CollectionInviteResponse(
        @Schema(description = "Token do convite, para compor o link de compartilhamento no frontend.", example = "Jt5f2K9pQmX3vL7nR8wZ1cB4dE6gH0aYsUiOoPzT9mN")
        String token
) {
}
