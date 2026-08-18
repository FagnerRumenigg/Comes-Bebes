package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.Follow;
import org.application.model.FollowId;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.repository.FollowRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private static final String NEW_FOLLOWER = "NEW_FOLLOWER";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional
    public void follow(UUID followerId, UUID followedId) {
        if (followerId.equals(followedId)) {
            throw new InvalidOperationException("CANNOT_FOLLOW_SELF", "Você não pode seguir a si mesmo.");
        }
        requireActiveUser(followerId);
        requireActiveUser(followedId);

        FollowId id = new FollowId(followerId, followedId);
        Optional<Follow> existing = followRepository.findById(id);
        boolean alreadyActive = existing.map(Follow::isActive).orElse(false);
        Follow follow = existing.orElseGet(() -> Follow.builder().followerId(followerId).followedId(followedId).build());
        follow.reactivate();
        followRepository.save(follow);

        if (!alreadyActive) {
            notifyNewFollower(followedId, followerId);
        }
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followedId) {
        requireActiveUser(followerId);
        followRepository.findById(new FollowId(followerId, followedId))
                .ifPresent(follow -> {
                    follow.remove(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
                    followRepository.save(follow);
                });
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followedId) {
        return followRepository.existsByFollowerIdAndFollowedIdAndDeletedAtIsNull(followerId, followedId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(UUID userId) {
        return followRepository.countByFollowedIdAndDeletedAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public long countFollowing(UUID userId) {
        return followRepository.countByFollowerIdAndDeletedAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public Page<User> listFollowers(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        Page<Follow> page = followRepository.findByFollowedIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
        return mapToUsers(page, Follow::getFollowerId);
    }

    @Transactional(readOnly = true)
    public Page<User> listFollowing(UUID userId, Pageable pageable) {
        requireActiveUser(userId);
        Page<Follow> page = followRepository.findByFollowerIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
        return mapToUsers(page, Follow::getFollowedId);
    }

    private Page<User> mapToUsers(Page<Follow> page, Function<Follow, UUID> idExtractor) {
        List<UUID> ids = page.getContent().stream().map(idExtractor).toList();
        Map<UUID, User> usersById = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<User> ordered = ids.stream().map(usersById::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, page.getPageable(), page.getTotalElements());
    }

    private void notifyNewFollower(UUID followedId, UUID followerId) {
        notificationRepository.save(UserNotification.builder()
                .id(UUID.randomUUID())
                .userId(followedId)
                .type(NEW_FOLLOWER)
                .actorId(followerId)
                .build());
    }

    private void requireActiveUser(UUID userId) {
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }
}
