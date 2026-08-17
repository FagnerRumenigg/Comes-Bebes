package org.application.controller.publication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.Publication;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.model.PublicationStatus;
import org.application.util.DateTimeConverter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import org.application.model.ReactionCode;

@Builder
@Schema(name = "PublicationResponse", description = "Dados públicos de uma publicação ativa.")
public record PublicationResponse(
        @Schema(description = "Identificador da publicação.", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Identificador do autor.", example = "4f5c2c9e-5d5b-4cc9-8f57-7e4dbd5b5b9a")
        UUID authorId,
        @Schema(description = "Tipo da publicação.", example = "DISH")
        PublicationType type,
        @Schema(description = "Visibilidade da publicação.", example = "PUBLIC")
        PublicationVisibility visibility,
        @Schema(description = "Título opcional da publicação.", example = "Lasanha à bolonhesa", nullable = true)
        String title,
        @Schema(description = "Descrição opcional.", example = "Receita de domingo", nullable = true)
        String description,
        @Schema(description = "Status atual da publicação.", example = "ACTIVE")
        PublicationStatus status,
        @Schema(description = "Data de publicação no timezone da aplicação.", example = "2026-08-08T12:00:00-03:00")
        OffsetDateTime publishedAt,
        String imageUrl,
        String authorUsername,
        String authorDisplayName,
        boolean showReactionCounts,
        Map<ReactionCode, Long> reactionTotals,
        List<ReactionCode> selectedReactions,
        boolean saved,
        long versionsCount,
        @Schema(description = "Publicação de origem; preenchido somente para MY_VERSION.", nullable = true)
        UUID originalPublicationId,
        boolean reportedByCurrentUser,
        RecipeResponse recipePreview,
        @Schema(description = "Indica se a publicação foi editada por um administrador.")
        boolean editedByAdmin,
        @Schema(description = "Data/hora em que a foto foi tirada, lida do EXIF automaticamente. "
                + "Nula quando a imagem não carrega essa informação (prints, PNGs, fotos reenviadas por apps que removem metadados).",
                example = "2026-08-15T14:32:07-03:00", nullable = true)
        OffsetDateTime photoTakenAt
) {

    public static PublicationResponse of(Publication publication, ZoneId zoneId) {
        return PublicationResponse.builder()
                .id(publication.getId())
                .authorId(publication.getAuthorId())
                .type(publication.getType())
                .visibility(publication.getVisibility())
                .title(publication.getTitle())
                .description(publication.getDescription())
                .status(publication.getStatus())
                .publishedAt(DateTimeConverter.toApplicationTime(publication.getPublishedAt(), zoneId))
                .photoTakenAt(DateTimeConverter.toApplicationTime(publication.getPhotoTakenAt(), zoneId))
                .build();
    }
}

