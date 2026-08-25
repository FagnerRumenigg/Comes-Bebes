package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.request.CreatePhotoValidationFeedbackRequest;
import org.application.model.PhotoValidationFeedback;
import org.application.model.UserStatus;
import org.application.repository.PhotoValidationFeedbackRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoValidationFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(PhotoValidationFeedbackService.class);

    private final PhotoValidationFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public void create(UUID reporterId, CreatePhotoValidationFeedbackRequest request) {
        userRepository.findByIdAndStatus(reporterId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        PhotoValidationFeedback feedback = feedbackRepository.save(PhotoValidationFeedback.builder()
                .id(UUID.randomUUID())
                .reporterId(reporterId)
                .reasonCode(request.reasonCode())
                .comment(request.comment())
                .build());
        log.info("event=photo_validation_feedback_created feedbackId={} reporterId={} reasonCode={}",
                feedback.getId(), reporterId, request.reasonCode());
    }
}
