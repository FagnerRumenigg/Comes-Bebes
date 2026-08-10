package org.application.controller.publication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.SavedPublication;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "SavedPublicationResponse", description = "Item da lista privada de publicações salvas.")
public record SavedPublicationResponse(
        UUID publicationId,
        OffsetDateTime savedAt
) {
    public static SavedPublicationResponse of(SavedPublication item) {
        return builder().publicationId(item.getPublicationId()).savedAt(item.getCreatedAt()).build();
    }
}
