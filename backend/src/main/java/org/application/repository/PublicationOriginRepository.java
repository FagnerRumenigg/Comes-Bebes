package org.application.repository;

import org.application.model.PublicationOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PublicationOriginRepository extends JpaRepository<PublicationOrigin, UUID> {
    boolean existsBySourceRecipeId(UUID sourceRecipeId);
}
