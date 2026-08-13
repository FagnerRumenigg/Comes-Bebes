package org.application.controller.patchnote.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.PatchNote;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "PatchNoteResponse", description = "Nota de versão publicada.")
public record PatchNoteResponse(
        UUID id,
        String title,
        String body,
        OffsetDateTime publishedAt
) {
    public static PatchNoteResponse of(PatchNote item) {
        return builder()
                .id(item.getId())
                .title(item.getTitle())
                .body(item.getBody())
                .publishedAt(item.getPublishedAt())
                .build();
    }
}
