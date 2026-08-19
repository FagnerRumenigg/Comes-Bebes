package org.application.controller.collection.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.CollectionVisibility;
import org.application.model.PublicationCollection;
import org.application.model.User;
import org.application.util.DateTimeConverter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Builder
@Schema(name = "CollectionResponse", description = "Coleção curada de publicações.")
public record CollectionResponse(
        @Schema(description = "Identificador único da coleção.", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "UUID do autor da coleção.", example = "71131447-a2a0-4996-a336-a8c3555bb327")
        UUID authorId,
        @Schema(description = "Nome público do autor.", example = "fagner")
        String authorUsername,
        @Schema(description = "Nome exibido do autor.", example = "Fagner")
        String authorDisplayName,
        @Schema(description = "Nome da coleção.", example = "Receitas de domingo")
        String name,
        @Schema(description = "Descrição opcional.", example = "Pratos para reunir a família", nullable = true)
        String description,
        @Schema(description = "Visibilidade da coleção.", example = "PUBLIC")
        CollectionVisibility visibility,
        @Schema(description = "Quantidade de publicações na coleção.", example = "8")
        long publicationsCount,
        @Schema(description = "Quantidade de seguidores da coleção.", example = "3")
        long followersCount,
        @Schema(description = "Indica se a conta autenticada segue esta coleção. Nulo para visitantes ou para o próprio autor.", nullable = true)
        Boolean followedByCurrentUser,
        @Schema(description = "Data de criação.")
        OffsetDateTime createdAt,
        @Schema(description = "Data da última atualização.")
        OffsetDateTime updatedAt
) {
    public static CollectionResponse of(PublicationCollection collection, ZoneId zoneId, User author,
                                         long publicationsCount, long followersCount, Boolean followedByCurrentUser) {
        return CollectionResponse.builder()
                .id(collection.getId())
                .authorId(collection.getAuthorId())
                .authorUsername(author == null ? null : author.getUsername())
                .authorDisplayName(author == null ? null : author.getDisplayName())
                .name(collection.getName())
                .description(collection.getDescription())
                .visibility(collection.getVisibility())
                .publicationsCount(publicationsCount)
                .followersCount(followersCount)
                .followedByCurrentUser(followedByCurrentUser)
                .createdAt(DateTimeConverter.toApplicationTime(collection.getCreatedAt(), zoneId))
                .updatedAt(DateTimeConverter.toApplicationTime(collection.getUpdatedAt(), zoneId))
                .build();
    }
}
