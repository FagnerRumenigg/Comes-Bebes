package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "CreatePublicationRequest", description = "Dados para criar uma publicação usando armazenamento local de imagem.")
public record CreatePublicationRequest(
        @Schema(description = "UUID do autor. Será substituído pelo usuário autenticado quando o Spring Security for integrado.", example = "4f5c2c9e-5d5b-4cc9-8f57-7e4dbd5b5b9a")
        @NotNull UUID authorId,
        @Schema(description = "Tipo da publicação.", example = "RECIPE", allowableValues = {"DISH", "RECIPE"})
        @NotBlank @Pattern(regexp = "DISH|RECIPE") String type,
        @Schema(description = "Visibilidade.", example = "PUBLIC")
        @NotBlank @Pattern(regexp = "PUBLIC|INTERNAL") String visibility,
        @Schema(description = "Título da publicação.", example = "Lasanha à bolonhesa", nullable = true)
        @Size(max = 150) String title,
        @Schema(description = "Descrição opcional.", example = "Receita de domingo", nullable = true)
        @Size(max = 2000) String description,
        @Schema(description = "URL da imagem que será baixada para o armazenamento local.", example = "https://example.com/images/lasanha.jpg")
        @NotBlank @Pattern(regexp = "(https?|file)://.+") String imageUrl,
        @Schema(description = "Dados da receita. Obrigatório quando type=RECIPE.", nullable = true)
        @Valid CreateRecipeRequest recipe
) {
}
