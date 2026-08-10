package org.application.repository;

import org.application.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    java.util.Optional<Recipe> findByPublicationIdAndDeletedAtIsNull(UUID publicationId);
}
