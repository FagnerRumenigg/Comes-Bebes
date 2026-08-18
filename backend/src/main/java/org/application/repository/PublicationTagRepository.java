package org.application.repository;

import org.application.model.PublicationTag;
import org.application.model.PublicationTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PublicationTagRepository extends JpaRepository<PublicationTag, PublicationTagId> {
    List<PublicationTag> findByPublicationIdOrderByCreatedAtAsc(UUID publicationId);
    void deleteByPublicationId(UUID publicationId);
}
