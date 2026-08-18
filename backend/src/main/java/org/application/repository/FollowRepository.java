package org.application.repository;

import org.application.model.Follow;
import org.application.model.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    Page<Follow> findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID followedId, Pageable pageable);
    Page<Follow> findByFollowerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID followerId, Pageable pageable);
    long countByFollowedIdAndDeletedAtIsNull(UUID followedId);
    long countByFollowerIdAndDeletedAtIsNull(UUID followerId);
    boolean existsByFollowerIdAndFollowedIdAndDeletedAtIsNull(UUID followerId, UUID followedId);
}
