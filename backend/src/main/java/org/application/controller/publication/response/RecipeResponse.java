package org.application.controller.publication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.application.model.RecipeIngredient;
import org.application.service.RecipeService.RecipeDetails;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(name = "RecipeResponse", description = "Receita completa com seus ingredientes.")
public record RecipeResponse(
        @Schema(description = "UUID da publicação que contém a receita.", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID publicationId,
        @Schema(description = "Quantidade produzida.", example = "6", nullable = true)
        BigDecimal yieldQuantity,
        @Schema(description = "Unidade do rendimento.", example = "porções", nullable = true)
        String yieldUnit,
        @Schema(description = "Modo de preparo, com um passo por linha.", example = "Preparar o molho.\nMontar a receita.")
        String instructions,
        @Schema(description = "Ingredientes ordenados.")
        List<IngredientResponse> ingredients
) {

    public static RecipeResponse of(RecipeDetails details) {
        return RecipeResponse.builder()
                .publicationId(details.recipe().getPublicationId())
                .yieldQuantity(details.recipe().getYieldQuantity())
                .yieldUnit(details.recipe().getYieldUnit())
                .instructions(details.recipe().getInstructions())
                .ingredients(details.ingredients().stream().map(IngredientResponse::of).toList())
                .build();
    }

    @Builder
    @Schema(name = "IngredientResponse", description = "Ingrediente da receita.")
    public record IngredientResponse(
            @Schema(example = "1") short position,
            @Schema(example = "Carne moída") String name,
            @Schema(example = "500", nullable = true) BigDecimal quantity,
            @Schema(example = "g", nullable = true) String unit,
            @Schema(example = "fresca", nullable = true) String note
    ) {
        public static IngredientResponse of(RecipeIngredient ingredient) {
            return IngredientResponse.builder()
                    .position(ingredient.getPosition())
                    .name(ingredient.getName())
                    .quantity(ingredient.getQuantity())
                    .unit(ingredient.getUnit())
                    .note(ingredient.getNote())
                    .build();
        }
    }
}

