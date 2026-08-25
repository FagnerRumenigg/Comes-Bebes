package org.application.service;

import org.application.controller.publication.request.ReactionRequest;
import org.application.model.*;
import org.application.repository.*;
import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.application.util.StringNormalizer;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {
    @Mock private PublicationReactionRepository reactionRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private ReactionTypeRepository reactionTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private java.time.Clock clock;
    @Mock private StringNormalizer normalizer;
    @InjectMocks private ReactionService service;

    @Test
    void shouldRejectReactionByPublicationAuthor() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Publication publication = Publication.builder().id(publicationId).authorId(userId).status(PublicationStatus.ACTIVE).build();
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));

        assertThatThrownBy(() -> service.apply(publicationId, new ReactionRequest(userId, "WOULD_EAT")))
                .isInstanceOf(InvalidOperationException.class);
        verifyNoInteractions(reactionTypeRepository, reactionRepository);
    }

    @Test
    void shouldSaveReactionForAnotherActiveUser() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Short typeId = 1;
        Publication publication = Publication.builder().id(publicationId).authorId(UUID.randomUUID()).status(PublicationStatus.ACTIVE).build();
        ReactionType type = mock(ReactionType.class);
        when(type.getId()).thenReturn(typeId);
        when(normalizer.normalize(" WOULD_EAT ")).thenReturn("would_eat");
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(reactionTypeRepository.findByCodeAndActiveTrue("WOULD_EAT")).thenReturn(Optional.of(type));
        when(reactionRepository.findById(any())).thenReturn(Optional.empty());

        service.apply(publicationId, new ReactionRequest(userId, " WOULD_EAT "));

        verify(reactionRepository).save(any(PublicationReaction.class));
    }

    @Test
    void shouldRejectNewReactionWhenUserAlreadyHasThreeActive() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Short typeId = 4;
        Publication publication = Publication.builder().id(publicationId).authorId(UUID.randomUUID()).status(PublicationStatus.ACTIVE).build();
        ReactionType type = mock(ReactionType.class);
        when(type.getId()).thenReturn(typeId);
        when(normalizer.normalize("HUNGRY")).thenReturn("hungry");
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(reactionTypeRepository.findByCodeAndActiveTrue("HUNGRY")).thenReturn(Optional.of(type));
        when(reactionRepository.findById(any())).thenReturn(Optional.empty());
        when(reactionRepository.countByPublicationIdAndUserIdAndDeletedAtIsNull(publicationId, userId)).thenReturn(3L);

        assertThatThrownBy(() -> service.apply(publicationId, new ReactionRequest(userId, "HUNGRY")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Você pode escolher até 3 reações para esta publicação.");
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void shouldAllowReactivatingAnAlreadySelectedReactionEvenAtTheLimit() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Publication publication = Publication.builder().id(publicationId).authorId(UUID.randomUUID()).status(PublicationStatus.ACTIVE).build();
        ReactionType type = mock(ReactionType.class);
        when(type.getId()).thenReturn((short) 1);
        PublicationReaction existing = PublicationReaction.builder().publicationId(publicationId).userId(userId).reactionTypeId((short) 1).build();
        when(normalizer.normalize("WOULD_EAT")).thenReturn("would_eat");
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(reactionTypeRepository.findByCodeAndActiveTrue("WOULD_EAT")).thenReturn(Optional.of(type));
        when(reactionRepository.findById(any())).thenReturn(Optional.of(existing));

        service.apply(publicationId, new ReactionRequest(userId, "WOULD_EAT"));

        verify(reactionRepository).save(existing);
        verify(reactionRepository, never()).countByPublicationIdAndUserIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void shouldRemoveExistingReaction() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Publication publication = Publication.builder().id(publicationId).authorId(UUID.randomUUID()).status(PublicationStatus.ACTIVE).build();
        ReactionType type = mock(ReactionType.class);
        when(type.getId()).thenReturn((short) 1);
        PublicationReaction reaction = PublicationReaction.builder().publicationId(publicationId).userId(userId).reactionTypeId((short) 1).build();
        when(normalizer.normalize("WOULD_EAT")).thenReturn("would_eat");
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(publication));
        when(publicationRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        // remove() busca por findByCode (sem exigir active=true): quem reagiu antes de um
        // tipo ser retirado do catálogo continua podendo tirar a própria reação.
        when(reactionTypeRepository.findByCode("WOULD_EAT")).thenReturn(Optional.of(type));
        when(reactionRepository.findById(any())).thenReturn(Optional.of(reaction));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        service.remove(publicationId, new ReactionRequest(userId, "WOULD_EAT"));

        verify(reactionRepository).save(reaction);
        assertThat(reaction.getDeletedAt()).isNotNull();
    }
}
