package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ReactionRequest", description = "Código da reação aplicada pelo usuário autenticado.")
public record ReactionRequest(
        @Schema(hidden = true) java.util.UUID userId,
        @Schema(description = "Código técnico da reação.", example = "WOULD_EAT", allowableValues = {"WOULD_EAT", "WANT_TO_MAKE", "COMFORT_FOOD"})
        @NotBlank String reactionCode
) {
}
