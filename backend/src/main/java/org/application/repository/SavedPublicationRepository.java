package org.application.repository;

import org.application.model.SavedPublication;
import org.application.model.SavedPublicationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface SavedPublicationRepository extends JpaRepository<SavedPublication, SavedPublicationId> {
    Page<SavedPublication> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    boolean existsByUserIdAndPublicationIdAndDeletedAtIsNull(UUID userId, UUID publicationId);
}
