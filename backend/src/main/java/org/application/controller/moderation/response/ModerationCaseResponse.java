package org.application.controller.moderation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.ModerationCase;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(name = "ModerationCaseResponse", description = "Caso de moderação da fila administrativa.")
public record ModerationCaseResponse(
        UUID id,
        UUID publicationId,
        String status,
        int reportCountAtOpen,
        OffsetDateTime openedAt,
        @Schema(nullable = true) UUID reviewedBy,
        @Schema(nullable = true) OffsetDateTime reviewedAt,
        @Schema(nullable = true) String decisionNote
) {
    public static ModerationCaseResponse of(ModerationCase item) {
        return builder()
                .id(item.getId()).publicationId(item.getPublicationId()).status(item.getStatus())
                .reportCountAtOpen(item.getReportCountAtOpen()).openedAt(item.getOpenedAt())
                .reviewedBy(item.getReviewedBy()).reviewedAt(item.getReviewedAt()).decisionNote(item.getDecisionNote())
                .build();
    }
}
