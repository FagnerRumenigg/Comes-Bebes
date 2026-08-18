package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "CreateMyVersionRequest", description = "Dados para criar uma versão própria de uma receita existente.")
public record CreateMyVersionRequest(
        @Schema(description = "UUID do autor da versão.", example = "4f5c2c9e-5d5b-4cc9-8f57-7e4dbd5b5b9a")
        @NotNull UUID authorId,
        @Schema(description = "Visibilidade da versão.", example = "PUBLIC")
        @NotBlank @Pattern(regexp = "PUBLIC|INTERNAL") String visibility,
        @Schema(description = "Complemento obrigatório do título original.", example = "versão com berinjela")
        @NotBlank @Size(max = 100) String titleSuffix,
        @Schema(description = "Resumo opcional das alterações.", example = "Troquei a carne por berinjela.", nullable = true)
        @Size(max = 2000) String changeSummary,
        @Schema(description = "URL fictícia usada pelo storage local.", example = "https://local.test/images/versao.jpg")
        @NotBlank @Pattern(regexp = "https?://.+") String imageUrl,
        @Schema(description = "Receita completa da versão.")
        @NotNull @Valid CreateRecipeRequest recipe,
        @Schema(description = "Tags de alimento, no máximo 5.", nullable = true)
        @Size(max = 5) List<@NotBlank @Size(max = 40) String> tags
) {
    public CreateMyVersionRequest(UUID authorId, String visibility, String titleSuffix, String changeSummary, String imageUrl, CreateRecipeRequest recipe) {
        this(authorId, visibility, titleSuffix, changeSummary, imageUrl, recipe, null);
    }
}
