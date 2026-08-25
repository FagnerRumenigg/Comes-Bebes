package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.Publication;
import org.application.model.PublicationStatus;
import org.application.model.SavedPublicationId;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.repository.PublicationRepository;
import org.application.repository.SavedPublicationRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.application.model.SavedPublication;

@Service
@RequiredArgsConstructor
public class SavedPublicationService {
    private static final String SAVED_YOUR_PUBLICATION = "SAVED_YOUR_PUBLICATION";

    private final SavedPublicationRepository savedPublicationRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional
    public void save(UUID publicationId, UUID userId) {
        saveAs(publicationId, userId);
    }

    @Transactional
    public void saveAs(UUID publicationId, UUID userId) {
        Publication publication = validate(publicationId, userId);
        SavedPublicationId id = new SavedPublicationId(userId, publicationId);
        Optional<SavedPublication> existing = savedPublicationRepository.findById(id);
        boolean alreadyActive = existing.map(sp -> sp.getDeletedAt() == null).orElse(false);
        SavedPublication saved = existing.orElseGet(() -> SavedPublication.builder()
                .userId(userId)
                .publicationId(publicationId)
                .build());
        saved.reactivate();
        savedPublicationRepository.save(saved);

        if (!alreadyActive) {
            notifySaved(publication, userId);
        }
    }

    private void notifySaved(Publication publication, UUID userId) {
        if (publication.getAuthorId().equals(userId)) return;
        userRepository.findByIdAndStatus(publication.getAuthorId(), UserStatus.ACTIVE)
                .filter(User::isNotifyOnSaved)
                .ifPresent(author -> notificationRepository.save(UserNotification.builder()
                        .id(UUID.randomUUID())
                        .userId(author.getId())
                        .type(SAVED_YOUR_PUBLICATION)
                        .actorId(userId)
                        .publicationId(publication.getId())
                        .build()));
    }

    @Transactional
    public void remove(UUID publicationId, UUID userId) {
        removeAs(publicationId, userId);
    }

    @Transactional
    public void removeAs(UUID publicationId, UUID userId) {
        validate(publicationId, userId);
        savedPublicationRepository.findById(new SavedPublicationId(userId, publicationId))
                .ifPresent(saved -> {
                    saved.remove(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
                    savedPublicationRepository.save(saved);
                });
    }

    @Transactional(readOnly = true)
    public Page<SavedPublication> list(UUID userId, Pageable pageable) {
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        return savedPublicationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
    }

    private Publication validate(UUID publicationId, UUID userId) {
        Publication publication = publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        return publication;
    }
}
