package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(name = "MarkPublicationsViewedRequest", description = "IDs de publicações visualizadas pelo usuário autenticado, registrados em lote.")
public record MarkPublicationsViewedRequest(
        @NotEmpty
        @Schema(description = "IDs das publicações visualizadas.", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
        List<UUID> publicationIds
) {
}
