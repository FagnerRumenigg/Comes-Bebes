package org.application.repository;

import org.application.model.PublicationView;
import org.application.model.PublicationViewId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PublicationViewRepository extends JpaRepository<PublicationView, PublicationViewId> {
    boolean existsByUserIdAndPublicationId(UUID userId, UUID publicationId);
    List<PublicationView> findByUserIdAndPublicationIdIn(UUID userId, Collection<UUID> publicationIds);
}
