package org.application.repository;

import org.application.model.FeedbackSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, UUID> {

    Page<FeedbackSubmission> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
