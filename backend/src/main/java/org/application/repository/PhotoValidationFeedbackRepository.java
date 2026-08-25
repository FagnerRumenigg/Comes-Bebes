package org.application.repository;

import org.application.model.PhotoValidationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PhotoValidationFeedbackRepository extends JpaRepository<PhotoValidationFeedback, UUID> {
}
