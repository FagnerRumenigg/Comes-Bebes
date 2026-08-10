package org.application.repository;

import org.application.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {

    List<RecipeIngredient> findByRecipeIdAndDeletedAtIsNullOrderByPositionAsc(UUID recipeId);
}
