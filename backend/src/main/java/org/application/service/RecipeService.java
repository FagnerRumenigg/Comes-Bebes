package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.Recipe;
import org.application.model.RecipeIngredient;
import org.application.repository.RecipeIngredientRepository;
import org.application.repository.RecipeRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public RecipeDetails findDetails(UUID publicationId) {
        Recipe recipe = recipeRepository.findByPublicationIdAndDeletedAtIsNull(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("RECIPE_NOT_FOUND", "Receita não encontrada."));
        List<RecipeIngredient> ingredients = ingredientRepository
                .findByRecipeIdAndDeletedAtIsNullOrderByPositionAsc(publicationId);
        return new RecipeDetails(recipe, ingredients);
    }

    public record RecipeDetails(Recipe recipe, List<RecipeIngredient> ingredients) {
    }
}
