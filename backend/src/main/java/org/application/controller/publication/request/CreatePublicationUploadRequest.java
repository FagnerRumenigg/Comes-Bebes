package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreatePublicationUploadRequest", description = "Dados textuais de uma publicação com imagem multipart.")
public record CreatePublicationUploadRequest(
        @NotBlank @Pattern(regexp = "DISH|RECIPE") String type,
        @NotBlank @Pattern(regexp = "PUBLIC|INTERNAL") String visibility,
        @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Valid CreateRecipeRequest recipe
) {
}
