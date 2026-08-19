package org.application.controller.tag.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.Tag;

@Builder
@Schema(name = "TagResponse", description = "Tag de alimento associada a uma publicação.")
public record TagResponse(
        @Schema(description = "Nome de exibição da tag.", example = "Açaí")
        String name,
        @Schema(description = "Identificador normalizado, sem acento e em minúsculas.", example = "acai")
        String slug,
        @Schema(description = "Indica se a tag foi criada oficialmente pelo Comes&Bebes.")
        boolean official
) {
    public static TagResponse of(Tag tag) {
        return TagResponse.builder()
                .name(tag.getName())
                .slug(tag.getSlug())
                .official(tag.isOfficial())
                .build();
    }
}
