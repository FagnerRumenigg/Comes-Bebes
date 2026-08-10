package org.application.service;

import org.application.model.Recipe;
import org.application.repository.RecipeIngredientRepository;
import org.application.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeIngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void shouldReturnRecipeDetails() {
        UUID publicationId = UUID.randomUUID();
        Recipe recipe = Recipe.builder().publicationId(publicationId).instructions("Preparar.").build();
        when(recipeRepository.findByPublicationIdAndDeletedAtIsNull(publicationId)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findByRecipeIdAndDeletedAtIsNullOrderByPositionAsc(publicationId)).thenReturn(java.util.List.of());

        RecipeService.RecipeDetails details = recipeService.findDetails(publicationId);

        assertThat(details.recipe()).isSameAs(recipe);
        assertThat(details.ingredients()).isEmpty();
    }
}
