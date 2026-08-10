package org.application.repository;

import org.application.model.ModerationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ModerationCaseRepository extends JpaRepository<ModerationCase, UUID> {
    boolean existsByPublicationIdAndStatus(UUID publicationId, String status);
    List<ModerationCase> findByStatusOrderByOpenedAtAsc(String status);
    java.util.Optional<ModerationCase> findByPublicationIdAndStatus(UUID publicationId, String status);
}
