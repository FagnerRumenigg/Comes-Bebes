package org.application.controller.collection.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateCollectionRequest", description = "Dados para editar uma coleção de publicações.")
public record UpdateCollectionRequest(
        @Schema(description = "Novo nome da coleção.", example = "Receitas de domingo")
        @NotBlank @Size(max = 80) String name,
        @Schema(description = "Nova descrição.", example = "Pratos para reunir a família", nullable = true)
        @Size(max = 280) String description,
        @Schema(description = "Nova visibilidade.", example = "PUBLIC")
        @NotNull @Pattern(regexp = "PUBLIC|PRIVATE") String visibility
) {
}
