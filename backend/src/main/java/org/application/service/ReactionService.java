package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.request.ReactionRequest;
import org.application.model.Publication;
import org.application.model.PublicationReaction;
import org.application.model.PublicationReactionId;
import org.application.model.PublicationStatus;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.repository.PublicationReactionRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.ReactionTypeRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.model.UserStatus;
import org.application.service.exception.ResourceNotFoundException;
import org.application.service.exception.InvalidOperationException;
import org.application.util.StringNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private static final String REACTED_TO_YOUR_PUBLICATION = "REACTED_TO_YOUR_PUBLICATION";

    private final PublicationReactionRepository reactionRepository;
    private final PublicationRepository publicationRepository;
    private final ReactionTypeRepository reactionTypeRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final Clock clock;
    private final StringNormalizer stringNormalizer;

    @Transactional
    public void apply(java.util.UUID publicationId, ReactionRequest request) {
        apply(publicationId, request, request.userId());
    }

    private static final int MAX_REACTIONS_PER_USER = 3;

    @Transactional
    public void apply(java.util.UUID publicationId, ReactionRequest request, java.util.UUID userId) {
        Publication publication = validateActors(publicationId, userId);
        var type = reactionTypeRepository.findByCodeAndActiveTrue(stringNormalizer.normalize(request.reactionCode()).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("REACTION_TYPE_NOT_FOUND", "Tipo de reação não encontrado."));
        PublicationReactionId id = new PublicationReactionId(publicationId, userId, type.getId());
        PublicationReaction reaction = reactionRepository.findById(id).orElse(null);
        boolean alreadyActive = reaction != null && reaction.getDeletedAt() == null;
        if (!alreadyActive) {
            long activeCount = reactionRepository.countByPublicationIdAndUserIdAndDeletedAtIsNull(publicationId, userId);
            if (activeCount >= MAX_REACTIONS_PER_USER) {
                throw new InvalidOperationException("REACTION_LIMIT_REACHED", "Você pode escolher até 3 reações para esta publicação.");
            }
        }
        if (reaction == null) {
            reaction = PublicationReaction.builder()
                    .publicationId(publicationId)
                    .userId(userId)
                    .reactionTypeId(type.getId())
                    .build();
        }
        reaction.reactivate();
        reactionRepository.save(reaction);

        if (!alreadyActive) {
            notifyReacted(publication, userId);
        }
    }

    private void notifyReacted(Publication publication, java.util.UUID userId) {
        userRepository.findByIdAndStatus(publication.getAuthorId(), UserStatus.ACTIVE)
                .filter(User::isNotifyOnReacted)
                .ifPresent(author -> notificationRepository.save(UserNotification.builder()
                        .id(java.util.UUID.randomUUID())
                        .userId(author.getId())
                        .type(REACTED_TO_YOUR_PUBLICATION)
                        .actorId(userId)
                        .publicationId(publication.getId())
                        .build()));
    }

    @Transactional
    public void remove(java.util.UUID publicationId, ReactionRequest request) {
        remove(publicationId, request, request.userId());
    }

    @Transactional
    public void remove(java.util.UUID publicationId, ReactionRequest request, java.util.UUID userId) {
        validateActors(publicationId, userId);
        var type = reactionTypeRepository.findByCode(stringNormalizer.normalize(request.reactionCode()).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("REACTION_TYPE_NOT_FOUND", "Tipo de reação não encontrado."));
        reactionRepository.findById(new PublicationReactionId(publicationId, userId, type.getId()))
                .ifPresent(reaction -> {
                    reaction.remove(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
                    reactionRepository.save(reaction);
                });
    }

    private Publication validateActors(java.util.UUID publicationId, java.util.UUID userId) {
        Publication publication = publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        if (publication.getAuthorId().equals(userId)) {
            throw new InvalidOperationException("AUTHOR_SELF_REACTION", "O autor não pode reagir à própria publicação.");
        }
        return publication;
    }
}
