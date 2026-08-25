package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "CreatePublicationUploadRequest", description = "Dados textuais de uma publicação com imagem multipart.")
public record CreatePublicationUploadRequest(
        @NotBlank @Pattern(regexp = "DISH|RECIPE") String type,
        @NotBlank @Pattern(regexp = "PUBLIC|INTERNAL|PRIVATE") String visibility,
        @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Valid CreateRecipeRequest recipe,
        @Schema(description = "Tags de alimento, no máximo 5.", nullable = true)
        @Size(max = 5) List<@NotBlank @Size(max = 40) String> tags
) {
    public CreatePublicationUploadRequest(String type, String visibility, String title, String description, CreateRecipeRequest recipe) {
        this(type, visibility, title, description, recipe, null);
    }
}
