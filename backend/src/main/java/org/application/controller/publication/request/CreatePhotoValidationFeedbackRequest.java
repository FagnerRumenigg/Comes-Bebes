package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreatePhotoValidationFeedbackRequest", description = "Relato de que uma foto foi recusada por engano pelo validador de imagens.")
public record CreatePhotoValidationFeedbackRequest(
        @Schema(description = "Código do erro devolvido pela recusa (ex.: IMAGE_NOT_FOOD).", example = "IMAGE_NOT_FOOD")
        @NotBlank String reasonCode,
        @Schema(description = "Comentário livre da pessoa sobre por que acha que a recusa foi um engano.", example = "É um doce de leite, só está numa panela diferente.", nullable = true)
        @Size(max = 1000) String comment
) {
}
