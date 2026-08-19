package org.application.service;

import org.application.model.Publication;
import org.application.model.PublicationView;
import org.application.repository.PublicationRepository;
import org.application.repository.PublicationViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicationViewServiceTest {
    @Mock private PublicationViewRepository publicationViewRepository;
    @Mock private PublicationRepository publicationRepository;
    @InjectMocks private PublicationViewService service;

    @Test
    void shouldSaveOnlyNewViewsForExistingPublications() {
        UUID userId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        UUID alreadyViewedId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        List<UUID> requested = List.of(newId, alreadyViewedId, missingId);

        when(publicationRepository.findAllById(requested)).thenReturn(List.of(
                Publication.builder().id(newId).build(),
                Publication.builder().id(alreadyViewedId).build()));
        when(publicationViewRepository.findByUserIdAndPublicationIdIn(eq(userId), any()))
                .thenReturn(List.of(PublicationView.builder().userId(userId).publicationId(alreadyViewedId).build()));

        service.markViewed(userId, requested);

        ArgumentCaptor<List<PublicationView>> captor = ArgumentCaptor.forClass(List.class);
        verify(publicationViewRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPublicationId()).isEqualTo(newId);
        assertThat(captor.getValue().get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldDoNothingWhenAllRequestedIdsAreAlreadyViewedOrMissing() {
        UUID userId = UUID.randomUUID();
        UUID alreadyViewedId = UUID.randomUUID();
        List<UUID> requested = List.of(alreadyViewedId);

        when(publicationRepository.findAllById(requested)).thenReturn(List.of(
                Publication.builder().id(alreadyViewedId).build()));
        when(publicationViewRepository.findByUserIdAndPublicationIdIn(eq(userId), any()))
                .thenReturn(List.of(PublicationView.builder().userId(userId).publicationId(alreadyViewedId).build()));

        service.markViewed(userId, requested);

        verify(publicationViewRepository, never()).saveAll(any());
    }

    @Test
    void shouldDoNothingForEmptyRequest() {
        UUID userId = UUID.randomUUID();

        service.markViewed(userId, List.of());

        verifyNoInteractions(publicationRepository, publicationViewRepository);
    }
}
