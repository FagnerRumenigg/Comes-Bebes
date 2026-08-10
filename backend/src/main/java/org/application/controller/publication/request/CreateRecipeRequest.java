package org.application.controller.publication.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "CreateRecipeRequest", description = "Dados completos da receita.")
public record CreateRecipeRequest(
        @Schema(description = "Quantidade produzida.", example = "6", nullable = true)
        @Positive BigDecimal yieldQuantity,
        @Schema(description = "Unidade do rendimento.", example = "porções", nullable = true)
        @Size(max = 50) String yieldUnit,
        @Schema(description = "Modo de preparo, com um passo por linha.", example = "Preparar o molho.\nMontar a receita.\nAssar.")
        @NotBlank @Size(max = 10000) String instructions,
        @Schema(description = "Ingredientes ordenados da receita.")
        @NotEmpty @Valid List<CreateIngredientRequest> ingredients
) {
}
