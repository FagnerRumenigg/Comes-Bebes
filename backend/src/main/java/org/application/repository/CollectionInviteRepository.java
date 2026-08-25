package org.application.repository;

import org.application.model.CollectionInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectionInviteRepository extends JpaRepository<CollectionInvite, UUID> {
    Optional<CollectionInvite> findByCollectionIdAndRevokedAtIsNull(UUID collectionId);
    Optional<CollectionInvite> findByTokenAndRevokedAtIsNull(String token);
}
