package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@Schema(name = "CreateMyVersionUploadRequest", description = "Dados textuais da versão própria; a imagem é enviada em multipart.")
public record CreateMyVersionUploadRequest(
        @io.swagger.v3.oas.annotations.media.Schema(hidden = true) java.util.UUID authorId,
        @NotBlank @Pattern(regexp = "PUBLIC|INTERNAL") String visibility,
        @NotBlank @Size(max = 100) String titleSuffix,
        @Size(max = 2000) String changeSummary,
        @NotNull @Valid CreateRecipeRequest recipe
) {
    public CreateMyVersionUploadRequest(String visibility, String titleSuffix, String changeSummary, CreateRecipeRequest recipe) {
        this(null, visibility, titleSuffix, changeSummary, recipe);
    }
}
