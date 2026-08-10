package org.application.service;

import org.application.controller.publication.request.CreateReportRequest;
import org.application.model.*;
import org.application.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.application.util.StringNormalizer;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock private ReportRepository reportRepository;
    @Mock private ReportReasonRepository reasonRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppConfigRepository appConfigRepository;
    @Mock private ModerationCaseRepository moderationCaseRepository;
    @Mock private StringNormalizer normalizer;
    @InjectMocks private ReportService service;

    @Test
    void shouldCreatePendingReportBelowModerationThreshold() {
        UUID publicationId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        ReportReason reason = mock(ReportReason.class);
        when(reason.getId()).thenReturn((short) 1);
        when(normalizer.normalize("NOT_FOOD")).thenReturn("not_food");
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE))
                .thenReturn(Optional.of(Publication.builder().id(publicationId).authorId(UUID.randomUUID()).build()));
        when(userRepository.findByIdAndStatus(reporterId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(reporterId).build()));
        when(reasonRepository.findByCodeAndActiveTrue("NOT_FOOD")).thenReturn(Optional.of(reason));
        when(reportRepository.existsByPublicationIdAndReporterId(publicationId, reporterId)).thenReturn(false);
        AppConfig config = mock(AppConfig.class);
        when(config.getReportThreshold()).thenReturn(3);
        when(appConfigRepository.findById((short) 1)).thenReturn(Optional.of(config));
        when(reportRepository.countByPublicationIdAndResolution(publicationId, "PENDING")).thenReturn(1L);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(publicationId, new CreateReportRequest(reporterId, "NOT_FOOD", "not food"));

        verify(reportRepository).save(any(Report.class));
        verify(moderationCaseRepository, never()).save(any());
    }
}
