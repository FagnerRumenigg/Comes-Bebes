package org.application.repository;

import org.application.model.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatchNoteRepository extends JpaRepository<PatchNote, UUID> {
    Optional<PatchNote> findTopByOrderByPublishedAtDesc();

    List<PatchNote> findByPublishedAtAfterOrderByPublishedAtAsc(OffsetDateTime after);

    List<PatchNote> findAllByOrderByPublishedAtAsc();
}
