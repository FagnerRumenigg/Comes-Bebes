package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "UpdatePublicationRequest", description = "Dados para editar uma publicação ou converter seu tipo.")
public record UpdatePublicationRequest(
        @io.swagger.v3.oas.annotations.media.Schema(hidden = true) java.util.UUID authorId,
        @Schema(description = "Novo tipo. Se omitido, mantém o tipo atual.", example = "RECIPE", allowableValues = {"DISH", "RECIPE"}, nullable = true)
        @Pattern(regexp = "DISH|RECIPE") String type,
        @Schema(description = "Novo título.", example = "Lasanha especial", nullable = true)
        @Size(max = 255) String title,
        @Schema(description = "Nova descrição.", example = "Atualizada", nullable = true)
        @Size(max = 2000) String description,
        @Schema(description = "Nova visibilidade.", example = "PUBLIC", nullable = true)
        @Pattern(regexp = "PUBLIC|INTERNAL") String visibility,
        @Schema(description = "Receita obrigatória ao converter para RECIPE.")
        @Valid CreateRecipeRequest recipe
) {
}
