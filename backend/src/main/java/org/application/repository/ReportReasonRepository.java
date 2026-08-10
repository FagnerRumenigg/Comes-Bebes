package org.application.repository;

import org.application.model.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportReasonRepository extends JpaRepository<ReportReason, Short> {
    Optional<ReportReason> findByCodeAndActiveTrue(String code);
}
