package org.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.application.repository.ModerationCaseRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.ReportRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.controller.moderation.request.DecideModerationCaseRequest;
import org.application.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationCaseRepository caseRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private PublicationRepository publicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserNotificationRepository notificationRepository;
    @Mock
    private java.time.Clock clock;

    @InjectMocks
    private ModerationService moderationService;

    private ch.qos.logback.classic.Logger serviceLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUpLogging() {
        serviceLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ModerationService.class);
        appender = new ListAppender<>();
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDownLogging() {
        serviceLogger.detachAppender(appender);
    }

    @Test
    void shouldListPendingCasesInRepositoryOrder() {
        when(caseRepository.findByStatusOrderByOpenedAtAsc("PENDING")).thenReturn(List.of());

        assertThat(moderationService.pendingCases()).isEmpty();
    }

    @Test
    void shouldKeepPublicationAndResolveReports() {
        UUID caseId = UUID.randomUUID();
        UUID publicationId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        User reviewer = User.builder().id(reviewerId).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        org.application.model.ModerationCase item = org.application.model.ModerationCase.builder()
                .id(caseId).publicationId(publicationId).status("PENDING").build();
        Publication publication = Publication.builder().id(publicationId).status(PublicationStatus.UNDER_REVIEW).build();
        when(userRepository.findByIdAndStatus(reviewerId, UserStatus.ACTIVE)).thenReturn(Optional.of(reviewer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(item));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(reportRepository.findByModerationCaseId(caseId)).thenReturn(List.of());
        when(caseRepository.save(item)).thenReturn(item);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        var result = moderationService.decide(caseId, new DecideModerationCaseRequest(reviewerId, "KEPT", null));

        assertThat(result.getStatus()).isEqualTo("KEPT");
        assertThat(publication.getStatus()).isEqualTo(PublicationStatus.ACTIVE);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("event=moderation_case_decided", "caseId=" + caseId, "decision=KEPT");
    }
}
