package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.feedback.request.CreateFeedbackRequest;
import org.application.model.FeedbackSubmission;
import org.application.repository.FeedbackSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackSubmissionRepository feedbackSubmissionRepository;

    @Transactional
    public void submit(UUID userId, CreateFeedbackRequest request) {
        feedbackSubmissionRepository.save(FeedbackSubmission.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message(request.message().trim())
                .contactEmail(request.contactEmail() == null || request.contactEmail().isBlank()
                        ? null : request.contactEmail().trim())
                .build());
    }
}
