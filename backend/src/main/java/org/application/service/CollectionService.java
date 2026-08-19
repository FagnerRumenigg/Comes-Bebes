package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.CollectionFollow;
import org.application.model.CollectionFollowId;
import org.application.model.CollectionPublication;
import org.application.model.CollectionVisibility;
import org.application.model.Publication;
import org.application.model.PublicationCollection;
import org.application.model.PublicationStatus;
import org.application.model.UserStatus;
import org.application.repository.CollectionFollowRepository;
import org.application.repository.CollectionPublicationRepository;
import org.application.repository.PublicationCollectionRepository;
import org.application.repository.PublicationRepository;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final PublicationCollectionRepository collectionRepository;
    private final CollectionPublicationRepository collectionPublicationRepository;
    private final CollectionFollowRepository collectionFollowRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public PublicationCollection create(UUID authorId, String name, String description, CollectionVisibility visibility) {
        requireActiveUser(authorId);
        PublicationCollection collection = PublicationCollection.builder()
                .id(UUID.randomUUID())
                .authorId(authorId)
                .name(name.trim())
                .description(description)
                .visibility(visibility)
                .build();
        return collectionRepository.save(collection);
    }

    @Transactional
    public PublicationCollection update(UUID collectionId, UUID authorId, String name, String description, CollectionVisibility visibility) {
        PublicationCollection collection = requireOwned(collectionId, authorId);
        collection.update(name.trim(), description, visibility);
        return collectionRepository.save(collection);
    }

    @Transactional
    public void remove(UUID collectionId, UUID authorId) {
        PublicationCollection collection = requireOwned(collectionId, authorId);
        collection.remove(now());
        collectionRepository.save(collection);
    }

    @Transactional(readOnly = true)
    public PublicationCollection findAccessible(UUID collectionId, UUID viewerId) {
        PublicationCollection collection = collectionRepository.findByIdAndDeletedAtIsNull(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("COLLECTION_NOT_FOUND", "Coleção não encontrada."));
        if (!collection.isPublic() && !collection.getAuthorId().equals(viewerId)) {
            throw new ResourceNotFoundException("COLLECTION_NOT_FOUND", "Coleção não encontrada.");
        }
        return collection;
    }

    @Transactional(readOnly = true)
    public Page<PublicationCollection> listByAuthor(UUID authorId, UUID viewerId, Pageable pageable) {
        requireActiveUser(authorId);
        if (authorId.equals(viewerId)) {
            return collectionRepository.findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId, pageable);
        }
        return collectionRepository.findByAuthorIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtDesc(authorId, CollectionVisibility.PUBLIC, pageable);
    }

    @Transactional
    public void addPublication(UUID collectionId, UUID authorId, UUID publicationId) {
        requireOwned(collectionId, authorId);
        publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        if (collectionPublicationRepository.existsByCollectionIdAndPublicationId(collectionId, publicationId)) return;
        short nextPosition = (short) (collectionPublicationRepository.findMaxPosition(collectionId) + 1);
        collectionPublicationRepository.save(CollectionPublication.builder()
                .collectionId(collectionId)
                .publicationId(publicationId)
                .position(nextPosition)
                .build());
    }

    @Transactional
    public void removePublication(UUID collectionId, UUID authorId, UUID publicationId) {
        requireOwned(collectionId, authorId);
        collectionPublicationRepository.deleteByCollectionIdAndPublicationId(collectionId, publicationId);
    }

    @Transactional(readOnly = true)
    public long countPublications(UUID collectionId) {
        return collectionPublicationRepository.countByCollectionId(collectionId);
    }

    @Transactional(readOnly = true)
    public Page<Publication> listPublications(UUID collectionId, UUID viewerId, Pageable pageable) {
        findAccessible(collectionId, viewerId);
        Page<CollectionPublication> page = collectionPublicationRepository.findByCollectionIdOrderByPositionAsc(collectionId, pageable);
        List<UUID> ids = page.getContent().stream().map(CollectionPublication::getPublicationId).toList();
        Map<UUID, Publication> byId = publicationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Publication::getId, Function.identity()));
        List<Publication> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, page.getPageable(), page.getTotalElements());
    }

    @Transactional
    public void follow(UUID followerId, UUID collectionId) {
        requireActiveUser(followerId);
        PublicationCollection collection = findAccessible(collectionId, followerId);
        if (collection.getAuthorId().equals(followerId)) {
            throw new InvalidOperationException("CANNOT_FOLLOW_OWN_COLLECTION", "Você não pode seguir a própria coleção.");
        }
        CollectionFollowId id = new CollectionFollowId(followerId, collectionId);
        CollectionFollow follow = collectionFollowRepository.findById(id)
                .orElseGet(() -> CollectionFollow.builder().followerId(followerId).collectionId(collectionId).build());
        follow.reactivate();
        collectionFollowRepository.save(follow);
    }

    @Transactional
    public void unfollow(UUID followerId, UUID collectionId) {
        collectionFollowRepository.findById(new CollectionFollowId(followerId, collectionId))
                .ifPresent(follow -> {
                    follow.remove(now());
                    collectionFollowRepository.save(follow);
                });
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID collectionId) {
        if (followerId == null) return false;
        return collectionFollowRepository.existsByFollowerIdAndCollectionIdAndDeletedAtIsNull(followerId, collectionId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(UUID collectionId) {
        return collectionFollowRepository.countByCollectionIdAndDeletedAtIsNull(collectionId);
    }

    @Transactional(readOnly = true)
    public Page<PublicationCollection> listFollowed(UUID followerId, Pageable pageable) {
        Page<CollectionFollow> page = collectionFollowRepository.findByFollowerIdAndDeletedAtIsNullOrderByCreatedAtDesc(followerId, pageable);
        List<UUID> ids = page.getContent().stream().map(CollectionFollow::getCollectionId).toList();
        Map<UUID, PublicationCollection> byId = collectionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PublicationCollection::getId, Function.identity()));
        List<PublicationCollection> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, page.getPageable(), page.getTotalElements());
    }

    private PublicationCollection requireOwned(UUID collectionId, UUID authorId) {
        PublicationCollection collection = collectionRepository.findByIdAndDeletedAtIsNull(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("COLLECTION_NOT_FOUND", "Coleção não encontrada."));
        if (!collection.getAuthorId().equals(authorId)) {
            throw new InvalidOperationException("NOT_COLLECTION_AUTHOR", "Você não é o autor desta coleção.");
        }
        return collection;
    }

    private void requireActiveUser(UUID userId) {
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
