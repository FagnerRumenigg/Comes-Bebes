package org.application.repository;

import org.application.model.RetiredUsername;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RetiredUsernameRepository extends JpaRepository<RetiredUsername, UUID> {
    boolean existsByUsernameIgnoreCaseAndRetiredAtAfter(String username, OffsetDateTime cutoff);
}
