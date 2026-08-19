package org.application.repository;

import org.application.model.CollectionPublication;
import org.application.model.CollectionPublicationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CollectionPublicationRepository extends JpaRepository<CollectionPublication, CollectionPublicationId> {
    Page<CollectionPublication> findByCollectionIdOrderByPositionAsc(UUID collectionId, Pageable pageable);
    long countByCollectionId(UUID collectionId);
    boolean existsByCollectionIdAndPublicationId(UUID collectionId, UUID publicationId);
    void deleteByCollectionIdAndPublicationId(UUID collectionId, UUID publicationId);

    @Query("select coalesce(max(cp.position), -1) from CollectionPublication cp where cp.collectionId = :collectionId")
    int findMaxPosition(@Param("collectionId") UUID collectionId);
}
