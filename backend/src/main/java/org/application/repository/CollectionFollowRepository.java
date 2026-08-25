package org.application.repository;

import org.application.model.CollectionFollow;
import org.application.model.CollectionFollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollectionFollowRepository extends JpaRepository<CollectionFollow, CollectionFollowId> {
    Page<CollectionFollow> findByFollowerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID followerId, Pageable pageable);
    Page<CollectionFollow> findByCollectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID collectionId, Pageable pageable);
    long countByCollectionIdAndDeletedAtIsNull(UUID collectionId);
    boolean existsByFollowerIdAndCollectionIdAndDeletedAtIsNull(UUID followerId, UUID collectionId);
}
