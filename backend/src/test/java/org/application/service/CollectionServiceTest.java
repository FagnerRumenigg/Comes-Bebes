package org.application.service;

import org.application.model.CollectionFollow;
import org.application.model.CollectionFollowId;
import org.application.model.CollectionInvite;
import org.application.model.CollectionPublication;
import org.application.model.CollectionVisibility;
import org.application.model.Publication;
import org.application.model.PublicationCollection;
import org.application.model.PublicationStatus;
import org.application.model.PublicationType;
import org.application.model.PublicationVisibility;
import org.application.model.User;
import org.application.model.UserStatus;
import org.application.repository.CollectionFollowRepository;
import org.application.repository.CollectionInviteRepository;
import org.application.repository.CollectionPublicationRepository;
import org.application.repository.PublicationCollectionRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {
    @Mock private PublicationCollectionRepository collectionRepository;
    @Mock private CollectionPublicationRepository collectionPublicationRepository;
    @Mock private CollectionFollowRepository collectionFollowRepository;
    @Mock private CollectionInviteRepository collectionInviteRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;
    @InjectMocks private CollectionService service;

    /**
     * Regressão: o dono pode adicionar a própria publicação PRIVATE à própria coleção
     * pública. A coleção continua acessível a qualquer um, mas a publicação PRIVATE
     * dentro dela precisa continuar invisível pra quem não é o autor (produto5.md v5 §6.4)
     * — sem isso, "Só para mim" vazaria pela coleção.
     */
    @Test
    void shouldHidePrivatePublicationInsidePublicCollectionFromStranger() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        UUID privateId = UUID.randomUUID();

        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.PUBLIC).build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));

        var pageable = org.springframework.data.domain.Pageable.unpaged();
        List<CollectionPublication> links = List.of(
                CollectionPublication.builder().collectionId(collectionId).publicationId(publicId).position((short) 0).build(),
                CollectionPublication.builder().collectionId(collectionId).publicationId(privateId).position((short) 1).build());
        when(collectionPublicationRepository.findByCollectionIdOrderByPositionAsc(collectionId, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(links));

        Publication publicPublication = publication(publicId, authorId, PublicationVisibility.PUBLIC);
        Publication privatePublication = publication(privateId, authorId, PublicationVisibility.PRIVATE);
        when(publicationRepository.findAllById(any())).thenReturn(List.of(publicPublication, privatePublication));

        var resultForStranger = service.listPublications(collectionId, strangerId, pageable);
        assertThat(resultForStranger.getContent()).containsExactly(publicPublication);

        var resultForAuthor = service.listPublications(collectionId, authorId, pageable);
        assertThat(resultForAuthor.getContent()).containsExactly(publicPublication, privatePublication);

        var resultForVisitor = service.listPublications(collectionId, null, pageable);
        assertThat(resultForVisitor.getContent()).containsExactly(publicPublication);
    }

    private Publication publication(UUID id, UUID authorId, PublicationVisibility visibility) {
        return Publication.builder()
                .id(id).authorId(authorId).type(PublicationType.DISH)
                .visibility(visibility).status(PublicationStatus.ACTIVE).build();
    }

    @Test
    void shouldHideSharedCollectionFromStrangerWithoutInvite() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionFollowRepository.existsByFollowerIdAndCollectionIdAndDeletedAtIsNull(strangerId, collectionId))
                .thenReturn(false);

        assertThatThrownBy(() -> service.findAccessible(collectionId, strangerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldAllowSharedCollectionForInvitedFollower() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionFollowRepository.existsByFollowerIdAndCollectionIdAndDeletedAtIsNull(invitedId, collectionId))
                .thenReturn(true);

        assertThat(service.findAccessible(collectionId, invitedId)).isSameAs(collection);
    }

    @Test
    void shouldCreateInviteLinkOnFirstRequestAndReuseAfter() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionInviteRepository.findByCollectionIdAndRevokedAtIsNull(collectionId)).thenReturn(Optional.empty());
        when(collectionInviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.getOrCreateInviteLink(collectionId, authorId);

        assertThat(token).isNotBlank();
        ArgumentCaptor<CollectionInvite> captor = ArgumentCaptor.forClass(CollectionInvite.class);
        verify(collectionInviteRepository).save(captor.capture());
        assertThat(captor.getValue().getCollectionId()).isEqualTo(collectionId);
        assertThat(captor.getValue().getToken()).isEqualTo(token);
    }

    @Test
    void shouldReuseExistingActiveInviteLink() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        CollectionInvite existing = CollectionInvite.builder().id(UUID.randomUUID())
                .collectionId(collectionId).token("existing-token").build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionInviteRepository.findByCollectionIdAndRevokedAtIsNull(collectionId)).thenReturn(Optional.of(existing));

        String token = service.getOrCreateInviteLink(collectionId, authorId);

        assertThat(token).isEqualTo("existing-token");
        verify(collectionInviteRepository, never()).save(any());
    }

    @Test
    void shouldRevokeOldLinkWhenRegenerating() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        CollectionInvite existing = CollectionInvite.builder().id(UUID.randomUUID())
                .collectionId(collectionId).token("old-token").build();
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionInviteRepository.findByCollectionIdAndRevokedAtIsNull(collectionId)).thenReturn(Optional.of(existing));
        when(collectionInviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(java.time.Instant.parse("2026-08-20T12:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);

        String newToken = service.regenerateInviteLink(collectionId, authorId);

        assertThat(newToken).isNotEqualTo("old-token");
        assertThat(existing.isActive()).isFalse();
        verify(collectionInviteRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void shouldRejectInvalidOrRevokedInviteToken() {
        when(collectionInviteRepository.findByTokenAndRevokedAtIsNull("bad-token")).thenReturn(Optional.empty());
        UUID viewerId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(viewerId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(viewerId).build()));

        assertThatThrownBy(() -> service.acceptInvite("bad-token", viewerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGrantAccessWhenAcceptingValidInvite() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        CollectionInvite invite = CollectionInvite.builder().id(UUID.randomUUID())
                .collectionId(collectionId).token("good-token").build();
        when(userRepository.findByIdAndStatus(viewerId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(viewerId).build()));
        when(collectionInviteRepository.findByTokenAndRevokedAtIsNull("good-token")).thenReturn(Optional.of(invite));
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));
        when(collectionFollowRepository.findById(new CollectionFollowId(viewerId, collectionId))).thenReturn(Optional.empty());
        when(collectionFollowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PublicationCollection accessed = service.acceptInvite("good-token", viewerId);

        assertThat(accessed).isSameAs(collection);
        ArgumentCaptor<CollectionFollow> captor = ArgumentCaptor.forClass(CollectionFollow.class);
        verify(collectionFollowRepository).save(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(viewerId);
        assertThat(captor.getValue().getCollectionId()).isEqualTo(collectionId);
    }

    @Test
    void shouldNotCreateFollowRowWhenOwnerAcceptsOwnInvite() {
        UUID collectionId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        PublicationCollection collection = PublicationCollection.builder()
                .id(collectionId).authorId(authorId).name("Doces").visibility(CollectionVisibility.SHARED).build();
        CollectionInvite invite = CollectionInvite.builder().id(UUID.randomUUID())
                .collectionId(collectionId).token("owner-token").build();
        when(userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE))
                .thenReturn(Optional.of(User.builder().id(authorId).build()));
        when(collectionInviteRepository.findByTokenAndRevokedAtIsNull("owner-token")).thenReturn(Optional.of(invite));
        when(collectionRepository.findByIdAndDeletedAtIsNull(collectionId)).thenReturn(Optional.of(collection));

        service.acceptInvite("owner-token", authorId);

        verify(collectionFollowRepository, never()).save(any());
    }
}
