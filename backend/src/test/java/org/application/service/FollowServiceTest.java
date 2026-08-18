package org.application.service;

import org.application.model.Follow;
import org.application.model.FollowId;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.repository.FollowRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {
    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserNotificationRepository notificationRepository;
    @Mock private Clock clock;
    @InjectMocks private FollowService service;

    private User activeUser(UUID id) {
        return User.builder().id(id).status(UserStatus.ACTIVE).build();
    }

    @Test
    void shouldRejectFollowingSelf() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.follow(userId, userId))
                .isInstanceOf(InvalidOperationException.class);
        verify(followRepository, never()).save(any());
    }

    @Test
    void shouldRejectFollowingWhenFollowedUserDoesNotExist() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        when(userRepository.findByIdAndStatus(followedId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(followerId, followedId))
                .isInstanceOf(RuntimeException.class);
        verify(followRepository, never()).save(any());
    }

    @Test
    void shouldCreateFollowAndNotifyOnFirstFollow() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        when(userRepository.findByIdAndStatus(followedId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followedId)));
        when(followRepository.findById(new FollowId(followerId, followedId))).thenReturn(Optional.empty());

        service.follow(followerId, followedId);

        verify(followRepository).save(any(Follow.class));
        verify(notificationRepository).save(any(UserNotification.class));
    }

    @Test
    void shouldNotNotifyAgainWhenAlreadyFollowing() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        when(userRepository.findByIdAndStatus(followedId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followedId)));
        Follow existingActive = Follow.builder().followerId(followerId).followedId(followedId).build();
        when(followRepository.findById(new FollowId(followerId, followedId))).thenReturn(Optional.of(existingActive));

        service.follow(followerId, followedId);

        verify(followRepository).save(existingActive);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldNotifyAgainWhenRefollowingAfterUnfollow() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        when(userRepository.findByIdAndStatus(followedId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followedId)));
        Follow removed = Follow.builder().followerId(followerId).followedId(followedId)
                .deletedAt(java.time.OffsetDateTime.now()).build();
        when(followRepository.findById(new FollowId(followerId, followedId))).thenReturn(Optional.of(removed));

        service.follow(followerId, followedId);

        verify(notificationRepository).save(any(UserNotification.class));
        assertThat(removed.isActive()).isTrue();
    }

    @Test
    void shouldRemoveExistingFollowOnUnfollow() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        Follow follow = Follow.builder().followerId(followerId).followedId(followedId).build();
        when(followRepository.findById(new FollowId(followerId, followedId))).thenReturn(Optional.of(follow));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-08-08T15:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        service.unfollow(followerId, followedId);

        verify(followRepository).save(follow);
        assertThat(follow.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldNoOpOnUnfollowWhenNeverFollowed() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(userRepository.findByIdAndStatus(followerId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(followerId)));
        when(followRepository.findById(new FollowId(followerId, followedId))).thenReturn(Optional.empty());

        service.unfollow(followerId, followedId);

        verify(followRepository, never()).save(any());
    }

    @Test
    void shouldReportIsFollowingFromRepository() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFollowedIdAndDeletedAtIsNull(followerId, followedId)).thenReturn(true);

        assertThat(service.isFollowing(followerId, followedId)).isTrue();
    }

    @Test
    void shouldListFollowersPreservingOrderAndPageMetadata() {
        UUID userId = UUID.randomUUID();
        UUID followerA = UUID.randomUUID();
        UUID followerB = UUID.randomUUID();
        Follow followA = Follow.builder().followerId(followerA).followedId(userId).build();
        Follow followB = Follow.builder().followerId(followerB).followedId(userId).build();
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(userId)));
        when(followRepository.findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, unpaged()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(followA, followB)));
        when(userRepository.findAllById(List.of(followerA, followerB)))
                .thenReturn(List.of(activeUser(followerB), activeUser(followerA)));

        var page = service.listFollowers(userId, unpaged());

        assertThat(page.getContent()).extracting(User::getId).containsExactly(followerA, followerB);
    }
}
