package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
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
        @Valid CreateRecipeRequest recipe,
        @Schema(description = "Nova lista de tags (no máximo 5), substituindo a anterior por completo. "
                + "Omitido/nulo mantém as tags atuais; lista vazia remove todas.", nullable = true)
        @Size(max = 5) List<@NotBlank @Size(max = 40) String> tags
) {
    public UpdatePublicationRequest(UUID authorId, String type, String title, String description, String visibility, CreateRecipeRequest recipe) {
        this(authorId, type, title, description, visibility, recipe, null);
    }
}
