package org.application.repository;

import org.application.model.CollectionPublication;
import org.application.model.CollectionPublicationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CollectionPublicationRepository extends JpaRepository<CollectionPublication, CollectionPublicationId> {
    Page<CollectionPublication> findByCollectionIdOrderByPositionAsc(UUID collectionId, Pageable pageable);
    long countByCollectionId(UUID collectionId);
    boolean existsByCollectionIdAndPublicationId(UUID collectionId, UUID publicationId);
    void deleteByCollectionIdAndPublicationId(UUID collectionId, UUID publicationId);

    @Query("select coalesce(max(cp.position), -1) from CollectionPublication cp where cp.collectionId = :collectionId")
    int findMaxPosition(@Param("collectionId") UUID collectionId);

    // Capa do cartão de coleção: sempre o último prato adicionado (posição mais
    // alta), não o primeiro — Pageable(0,1) limita a 1 linha.
    @Query("""
            select p.gcsObjectName from CollectionPublication cp
            join Publication p on p.id = cp.publicationId
            where cp.collectionId = :collectionId and p.status = org.application.model.PublicationStatus.ACTIVE
            order by cp.position desc
            """)
    List<String> findCoverImageObjectNames(@Param("collectionId") UUID collectionId, Pageable pageable);
}
