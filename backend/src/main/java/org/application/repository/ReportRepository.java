package org.application.repository;

import org.application.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    boolean existsByPublicationIdAndReporterId(UUID publicationId, UUID reporterId);
    boolean existsByPublicationIdAndReporterIdAndResolution(UUID publicationId, UUID reporterId, String resolution);
    long countByPublicationIdAndResolution(UUID publicationId, String resolution);
    List<Report> findByModerationCaseId(UUID moderationCaseId);
    List<Report> findByPublicationIdAndResolution(UUID publicationId, String resolution);
}
