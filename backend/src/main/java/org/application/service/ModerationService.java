package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.moderation.request.DecideModerationCaseRequest;
import org.application.model.UserNotification;
import org.application.repository.ModerationCaseRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.ReportRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    private final ModerationCaseRepository caseRepository;
    private final ReportRepository reportRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<org.application.model.ModerationCase> pendingCases() {
        return caseRepository.findByStatusOrderByOpenedAtAsc("PENDING").stream()
                .sorted(java.util.Comparator
                        .comparingInt(this::priorityScore).reversed()
                        .thenComparing(org.application.model.ModerationCase::getOpenedAt))
                .toList();
    }

    private int priorityScore(org.application.model.ModerationCase item) {
        int score = item.getReportCountAtOpen() * 10;
        return reportRepository.findByModerationCaseId(item.getId()).stream()
                .mapToInt(report -> score + riskWeight(report.getReasonId()))
                .max()
                .orElse(score);
    }

    private int riskWeight(Short reasonId) {
        if (reasonId == null) return 0;
        return switch (reasonId) {
            case 6 -> 40; // conteúdo perigoso ou ilegal
            case 2, 3 -> 30; // pessoa identificável ou conteúdo ofensivo
            case 5 -> 20; // autoria
            case 4 -> 10; // propaganda
            default -> 0;
        };
    }

    @Transactional(readOnly = true)
    public org.application.model.ModerationCase find(UUID caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("MODERATION_CASE_NOT_FOUND", "Caso de moderação não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<org.application.controller.moderation.response.ReportEvidenceResponse> evidence(UUID caseId) {
        return reportRepository.findByModerationCaseId(caseId).stream()
                .map(org.application.controller.moderation.response.ReportEvidenceResponse::of)
                .toList();
    }

    @Transactional
    public org.application.model.ModerationCase decide(UUID caseId, DecideModerationCaseRequest request) {
        return decide(caseId, request, request.reviewerId());
    }

    @Transactional
    public org.application.model.ModerationCase decide(UUID caseId, DecideModerationCaseRequest request, UUID reviewerId) {
        var reviewer = userRepository.findByIdAndStatus(reviewerId, org.application.model.UserStatus.ACTIVE)
                .filter(user -> user.getRole() == org.application.model.UserRole.ADMIN)
                .orElseThrow(() -> new InvalidOperationException("Somente ADMIN pode decidir casos de moderação."));
        var item = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Caso de moderação não encontrado."));
        if (!"PENDING".equals(item.getStatus())) {
            throw new InvalidOperationException("O caso de moderação já foi decidido.");
        }
        String decision = request.decision().trim().toUpperCase();
        if (!List.of("KEPT", "HIDDEN", "REMOVED").contains(decision)) {
            throw new InvalidOperationException("Decisão de moderação inválida.");
        }
        if (!"KEPT".equals(decision) && (request.decisionNote() == null || request.decisionNote().isBlank())) {
            throw new InvalidOperationException("A justificativa é obrigatória para ocultar ou remover conteúdo.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        var publication = publicationRepository.findById(item.getPublicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Publicação do caso não encontrada."));
        if ("KEPT".equals(decision)) {
            publication.changeStatus(org.application.model.PublicationStatus.ACTIVE);
        } else if ("HIDDEN".equals(decision)) {
            publication.changeStatus(org.application.model.PublicationStatus.HIDDEN);
        } else {
            publication.remove(now);
        }
        item.decide(decision, reviewer.getId(), now, request.decisionNote());
        var reports = reportRepository.findByModerationCaseId(caseId);
        reports.forEach(report -> report.resolve("KEPT".equals(decision) ? "REJECTED" : "UPHELD", now));
        reportRepository.saveAll(reports);
        if ("KEPT".equals(decision)) {
            reports.forEach(report -> notificationRepository.save(UserNotification.builder()
                    .id(UUID.randomUUID()).userId(report.getReporterId()).type("REPORT_REJECTED_WARNING")
                    .moderationCaseId(caseId).publicationId(item.getPublicationId()).build()));
        }
        publicationRepository.save(publication);
        var saved = caseRepository.save(item);
        log.info("event=moderation_case_decided caseId={} publicationId={} reviewerId={} decision={}",
                caseId, item.getPublicationId(), reviewer.getId(), decision);
        return saved;
    }
}
