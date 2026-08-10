package org.application.repository;

import org.application.model.PublicationReaction;
import org.application.model.PublicationReactionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationReactionRepository extends JpaRepository<PublicationReaction, PublicationReactionId> {
    java.util.List<PublicationReaction> findByPublicationIdAndDeletedAtIsNull(java.util.UUID publicationId);
}
