package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "CreateIngredientRequest", description = "Ingrediente estruturado de uma receita.")
public record CreateIngredientRequest(
        @Schema(description = "Posição do ingrediente.", example = "1")
        @NotNull @Positive Short position,
        @Schema(description = "Nome do ingrediente.", example = "Carne moída")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Quantidade, opcional para 'a gosto'.", example = "500", nullable = true)
        @Positive BigDecimal quantity,
        @Schema(description = "Unidade livre.", example = "g", nullable = true)
        @Size(max = 50) String unit,
        @Schema(description = "Observação opcional.", example = "fresca", nullable = true)
        @Size(max = 255) String note
) {
}
