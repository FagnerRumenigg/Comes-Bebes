package org.application.service;

import org.application.model.*;
import org.application.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.data.domain.PageImpl;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedPublicationServiceTest {
    @Mock private SavedPublicationRepository savedRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private java.time.Clock clock;
    @InjectMocks private SavedPublicationService service;

    @Test
    void shouldSaveActivePublicationForActiveUser() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(Publication.builder().id(publicationId).build()));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(savedRepository.findById(any())).thenReturn(Optional.empty());

        service.save(publicationId, userId);

        verify(savedRepository).save(any(SavedPublication.class));
    }

    @Test
    void shouldRejectSaveForInactiveUser() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(Publication.builder().id(publicationId).build()));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(publicationId, userId))
                .isInstanceOf(RuntimeException.class);
        verify(savedRepository, never()).save(any());
    }

    @Test
    void shouldListSavedPublicationsForActiveUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(savedRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.list(userId, org.springframework.data.domain.Pageable.unpaged())).isEmpty();
    }

    @Test
    void shouldRemoveExistingSavedPublication() {
        UUID publicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)).thenReturn(Optional.of(Publication.builder().id(publicationId).build()));
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(User.builder().id(userId).build()));
        SavedPublication saved = SavedPublication.builder().publicationId(publicationId).userId(userId).build();
        when(savedRepository.findById(any())).thenReturn(Optional.of(saved));
        when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        service.remove(publicationId, userId);

        verify(savedRepository).save(saved);
        assertThat(saved.getDeletedAt()).isNotNull();
    }
}
