package org.application.controller.document.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.ContentDocument;
import org.application.util.DateTimeConverter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Builder
@Schema(name = "DocumentResponse", description = "Termos de Serviço, Política de Privacidade ou FAQ (docs/telas/09-configuracoes.html).")
public record DocumentResponse(
        @Schema(example = "TERMS_OF_SERVICE")
        String slug,
        String title,
        @Schema(description = "Parágrafos separados por linha em branco.")
        String body,
        OffsetDateTime updatedAt
) {
    public static DocumentResponse of(ContentDocument document, ZoneId zoneId) {
        return DocumentResponse.builder()
                .slug(document.getSlug())
                .title(document.getTitle())
                .body(document.getBody())
                .updatedAt(DateTimeConverter.toApplicationTime(document.getUpdatedAt(), zoneId))
                .build();
    }
}
