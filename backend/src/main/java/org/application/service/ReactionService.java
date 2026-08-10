package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.request.ReactionRequest;
import org.application.model.PublicationReaction;
import org.application.model.PublicationReactionId;
import org.application.model.PublicationStatus;
import org.application.repository.PublicationReactionRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.ReactionTypeRepository;
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

    private final PublicationReactionRepository reactionRepository;
    private final PublicationRepository publicationRepository;
    private final ReactionTypeRepository reactionTypeRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final StringNormalizer stringNormalizer;

    @Transactional
    public void apply(java.util.UUID publicationId, ReactionRequest request) {
        apply(publicationId, request, request.userId());
    }

    @Transactional
    public void apply(java.util.UUID publicationId, ReactionRequest request, java.util.UUID userId) {
        validateActors(publicationId, userId);
        var type = reactionTypeRepository.findByCodeAndActiveTrue(stringNormalizer.normalize(request.reactionCode()).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("REACTION_TYPE_NOT_FOUND", "Tipo de reação não encontrado."));
        PublicationReactionId id = new PublicationReactionId(publicationId, userId, type.getId());
        PublicationReaction reaction = reactionRepository.findById(id)
                .orElseGet(() -> PublicationReaction.builder()
                        .publicationId(publicationId)
                        .userId(userId)
                        .reactionTypeId(type.getId())
                        .build());
        reaction.reactivate();
        reactionRepository.save(reaction);
    }

    @Transactional
    public void remove(java.util.UUID publicationId, ReactionRequest request) {
        remove(publicationId, request, request.userId());
    }

    @Transactional
    public void remove(java.util.UUID publicationId, ReactionRequest request, java.util.UUID userId) {
        validateActors(publicationId, userId);
        var type = reactionTypeRepository.findByCodeAndActiveTrue(stringNormalizer.normalize(request.reactionCode()).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("REACTION_TYPE_NOT_FOUND", "Tipo de reação não encontrado."));
        reactionRepository.findById(new PublicationReactionId(publicationId, userId, type.getId()))
                .ifPresent(reaction -> {
                    reaction.remove(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
                    reactionRepository.save(reaction);
                });
    }

    private void validateActors(java.util.UUID publicationId, java.util.UUID userId) {
        publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        if (publicationRepository.findById(publicationId).map(p -> p.getAuthorId().equals(userId)).orElse(false)) {
            throw new InvalidOperationException("AUTHOR_SELF_REACTION", "O autor não pode reagir à própria publicação.");
        }
    }
}
