package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.feedback.request.CreateFeedbackRequest;
import org.application.model.FeedbackSubmission;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserRole;
import org.application.model.UserStatus;
import org.application.repository.FeedbackSubmissionRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String NEW_FEEDBACK_RECEIVED = "NEW_FEEDBACK_RECEIVED";

    private final FeedbackSubmissionRepository feedbackSubmissionRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;

    @Transactional
    public void submit(UUID userId, CreateFeedbackRequest request) {
        feedbackSubmissionRepository.save(FeedbackSubmission.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message(request.message().trim())
                .contactEmail(request.contactEmail() == null || request.contactEmail().isBlank()
                        ? null : request.contactEmail().trim())
                .build());
        notifyAdmins(userId);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackSubmission> list(Pageable pageable) {
        return feedbackSubmissionRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // Sem preferência de usuário pra este tipo (diferente dos outros 5 avisos
    // da tela 12) — só existe pra quem administra o app, então fica sempre
    // ligado em vez de ganhar mais uma coluna em `users`.
    private void notifyAdmins(UUID submitterId) {
        for (User admin : userRepository.findByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)) {
            if (admin.getId().equals(submitterId)) continue;
            notificationRepository.save(UserNotification.builder()
                    .id(UUID.randomUUID())
                    .userId(admin.getId())
                    .type(NEW_FEEDBACK_RECEIVED)
                    .actorId(submitterId)
                    .build());
        }
    }
}
