package org.application.repository;

import org.application.model.CollectionVisibility;
import org.application.model.PublicationCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PublicationCollectionRepository extends JpaRepository<PublicationCollection, UUID> {
    Optional<PublicationCollection> findByIdAndDeletedAtIsNull(UUID id);
    Page<PublicationCollection> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID authorId, Pageable pageable);
    Page<PublicationCollection> findByAuthorIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtDesc(UUID authorId, CollectionVisibility visibility, Pageable pageable);
}
