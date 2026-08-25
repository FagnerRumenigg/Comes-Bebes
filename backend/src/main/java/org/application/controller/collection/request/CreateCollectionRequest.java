package org.application.controller.collection.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateCollectionRequest", description = "Dados para criar uma coleção de publicações.")
public record CreateCollectionRequest(
        @Schema(description = "Nome da coleção.", example = "Receitas de domingo")
        @NotBlank @Size(max = 80) String name,
        @Schema(description = "Descrição opcional.", example = "Pratos para reunir a família", nullable = true)
        @Size(max = 280) String description,
        @Schema(description = "Visibilidade da coleção.", example = "PUBLIC")
        @NotNull @Pattern(regexp = "PUBLIC|SHARED|PRIVATE") String visibility
) {
}
