package org.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.application.controller.publication.request.CreateReportRequest;
import org.application.model.*;
import org.application.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.application.util.StringNormalizer;
import org.slf4j.LoggerFactory;

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

    private ch.qos.logback.classic.Logger serviceLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUpLogging() {
        serviceLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ReportService.class);
        appender = new ListAppender<>();
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDownLogging() {
        serviceLogger.detachAppender(appender);
    }

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
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("event=report_created", "publicationId=" + publicationId, "reporterId=" + reporterId);
    }

    @Test
    void shouldOpenModerationCaseAndLogWhenThresholdIsReached() {
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
        when(config.getReportThreshold()).thenReturn(1);
        when(appConfigRepository.findById((short) 1)).thenReturn(Optional.of(config));
        when(reportRepository.countByPublicationIdAndResolution(publicationId, "PENDING")).thenReturn(1L);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moderationCaseRepository.existsByPublicationIdAndStatus(publicationId, "PENDING")).thenReturn(false);
        UUID moderationCaseId = UUID.randomUUID();
        ModerationCase moderationCase = ModerationCase.builder().id(moderationCaseId).publicationId(publicationId).build();
        when(moderationCaseRepository.findByPublicationIdAndStatus(publicationId, "PENDING"))
                .thenReturn(Optional.of(moderationCase));
        when(reportRepository.findByPublicationIdAndResolution(publicationId, "PENDING")).thenReturn(java.util.List.of());

        service.create(publicationId, new CreateReportRequest(reporterId, "NOT_FOOD", "not food"));

        verify(moderationCaseRepository).save(any(ModerationCase.class));
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(1).getFormattedMessage())
                .contains("event=moderation_case_opened", "caseId=" + moderationCaseId, "publicationId=" + publicationId);
    }
}
