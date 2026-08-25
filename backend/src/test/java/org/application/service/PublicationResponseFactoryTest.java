package org.application.service;

import org.application.model.Publication;
import org.application.model.PublicationReaction;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.model.ReactionCode;
import org.application.model.ReactionType;
import org.application.repository.PublicationOriginRepository;
import org.application.repository.PublicationReactionRepository;
import org.application.repository.PublicationViewRepository;
import org.application.repository.ReactionTypeRepository;
import org.application.repository.ReportRepository;
import org.application.repository.SavedPublicationRepository;
import org.application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicationResponseFactoryTest {
    @Mock private UserRepository userRepository;
    @Mock private PublicationReactionRepository reactionRepository;
    @Mock private ReactionTypeRepository reactionTypeRepository;
    @Mock private SavedPublicationRepository savedPublicationRepository;
    @Mock private PublicationViewRepository publicationViewRepository;
    @Mock private PublicationOriginRepository publicationOriginRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private RecipeService recipeService;
    @Mock private TagService tagService;
    @InjectMocks private PublicationResponseFactory factory;

    /**
     * Regressão: reações aplicadas antes de um tipo ser retirado do catálogo (produto5.md v5 §6.1)
     * continuam gravadas no banco com o código antigo (ex.: WOULD_EAT). ReactionCode.valueOf()
     * lança IllegalArgumentException para esses códigos porque não são mais valores do enum —
     * a leitura de qualquer publicação com uma dessas reações não pode derrubar a resposta inteira.
     */
    @Test
    void shouldIgnoreRetiredReactionCodesInsteadOfFailing() {
        UUID publicationId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID reactorId = UUID.randomUUID();
        Publication publication = Publication.builder()
                .id(publicationId)
                .authorId(authorId)
                .type(PublicationType.DISH)
                .visibility(PublicationVisibility.PUBLIC)
                .status(PublicationStatus.ACTIVE)
                .build();

        ReactionType retired = mock(ReactionType.class);
        when(retired.getId()).thenReturn((short) 1);
        when(retired.getCode()).thenReturn("WOULD_EAT");
        ReactionType active = mock(ReactionType.class);
        when(active.getId()).thenReturn((short) 2);
        when(active.getCode()).thenReturn("HUNGRY");

        PublicationReaction retiredReaction = PublicationReaction.builder()
                .publicationId(publicationId).userId(reactorId).reactionTypeId((short) 1).build();
        PublicationReaction activeReaction = PublicationReaction.builder()
                .publicationId(publicationId).userId(reactorId).reactionTypeId((short) 2).build();

        when(reactionRepository.findByPublicationIdAndDeletedAtIsNull(publicationId))
                .thenReturn(List.of(retiredReaction, activeReaction));
        when(reactionTypeRepository.findAllById(any())).thenReturn(List.of(retired, active));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());
        when(publicationOriginRepository.findById(publicationId)).thenReturn(Optional.empty());
        when(publicationOriginRepository.existsBySourceRecipeId(publicationId)).thenReturn(false);
        when(tagService.findByPublicationId(publicationId)).thenReturn(List.of());

        var response = factory.of(publication, ZoneId.of("UTC"), reactorId);

        assertThat(response.usedReactions()).containsExactly(ReactionCode.HUNGRY);
        assertThat(response.selectedReactions()).containsExactly(ReactionCode.HUNGRY);
    }
}
