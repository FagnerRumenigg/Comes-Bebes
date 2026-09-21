package org.application.controller.moderation.response;

import org.application.model.Report;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportEvidenceResponse(UUID id, UUID reporterId, Short reasonId, String description,
                                     String resolution, OffsetDateTime createdAt) {
    public static ReportEvidenceResponse of(Report report) {
        return new ReportEvidenceResponse(report.getId(), report.getReporterId(), report.getReasonId(),
                report.getDescription(), report.getResolution(), report.getCreatedAt());
    }
}
