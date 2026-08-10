package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.request.CreateReportRequest;
import org.application.model.ModerationCase;
import org.application.model.PublicationStatus;
import org.application.model.Report;
import org.application.repository.AppConfigRepository;
import org.application.repository.ModerationCaseRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.ReportReasonRepository;
import org.application.repository.ReportRepository;
import org.application.repository.UserRepository;
import org.application.model.UserStatus;
import org.application.service.exception.DuplicateResourceException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.application.util.StringNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportReasonRepository reasonRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final AppConfigRepository appConfigRepository;
    private final ModerationCaseRepository moderationCaseRepository;
    private final StringNormalizer stringNormalizer;

    @Transactional
    public void create(UUID publicationId, CreateReportRequest request) {
        create(publicationId, request, request.reporterId());
    }

    @Transactional
    public void create(UUID publicationId, CreateReportRequest request, UUID reporterId) {
        var publication = publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        userRepository.findByIdAndStatus(reporterId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Denunciante não encontrado."));
        if (publication.getAuthorId().equals(reporterId)) {
            throw new InvalidOperationException("AUTHOR_SELF_REPORT", "O autor não pode denunciar a própria publicação.");
        }
        if (reportRepository.existsByPublicationIdAndReporterId(publicationId, reporterId)) {
            throw new DuplicateResourceException("DUPLICATE_REPORT", "Esta publicação já foi denunciada por este usuário.");
        }
        var reason = reasonRepository.findByCodeAndActiveTrue(stringNormalizer.normalize(request.reasonCode()).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("REPORT_REASON_NOT_FOUND", "Motivo de denúncia não encontrado."));
        Report savedReport = reportRepository.save(Report.builder()
                .id(UUID.randomUUID())
                .publicationId(publicationId)
                .reporterId(reporterId)
                .reasonId(reason.getId())
                .description(request.description())
                .build());

        int threshold = appConfigRepository.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Configuração da aplicação não encontrada."))
                .getReportThreshold();
        long pendingReports = reportRepository.countByPublicationIdAndResolution(publicationId, "PENDING");
        if (pendingReports >= threshold && !moderationCaseRepository.existsByPublicationIdAndStatus(publicationId, "PENDING")) {
            moderationCaseRepository.save(ModerationCase.builder()
                    .id(UUID.randomUUID())
                    .publicationId(publicationId)
                    .reportCountAtOpen((int) pendingReports)
                    .build());
            var moderationCase = moderationCaseRepository.findByPublicationIdAndStatus(publicationId, "PENDING").orElseThrow();
            var reports = reportRepository.findByPublicationIdAndResolution(publicationId, "PENDING");
            reports.forEach(report -> report.assignToCase(moderationCase.getId()));
            reportRepository.saveAll(reports);
            publication.changeStatus(PublicationStatus.UNDER_REVIEW);
            publicationRepository.save(publication);
        }
    }
}
