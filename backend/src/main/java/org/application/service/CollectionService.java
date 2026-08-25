package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.CollectionFollow;
import org.application.model.CollectionFollowId;
import org.application.model.CollectionInvite;
import org.application.model.CollectionPublication;
import org.application.model.CollectionVisibility;
import org.application.model.Publication;
import org.application.model.PublicationCollection;
import org.application.model.PublicationStatus;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.model.UserStatus;
import org.application.repository.CollectionFollowRepository;
import org.application.repository.CollectionInviteRepository;
import org.application.repository.CollectionPublicationRepository;
import org.application.repository.PublicationCollectionRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.UserNotificationRepository;
import org.application.repository.UserRepository;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {
    private static final int INVITE_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String NEW_ITEM_IN_FOLLOWED_COLLECTION = "NEW_ITEM_IN_FOLLOWED_COLLECTION";
    private static final String COLLECTION_SHARED_WITH_YOU = "COLLECTION_SHARED_WITH_YOU";

    private final PublicationCollectionRepository collectionRepository;
    private final CollectionPublicationRepository collectionPublicationRepository;
    private final CollectionFollowRepository collectionFollowRepository;
    private final CollectionInviteRepository collectionInviteRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final SavedPublicationService savedPublicationService;
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
        if (collection.getAuthorId().equals(viewerId)) return collection;
        boolean visible = switch (collection.getVisibility()) {
            case PUBLIC -> true;
            // "Para quem eu escolher": só quem aceitou o convite (produto5.md v5 §6.3).
            case SHARED -> viewerId != null
                    && collectionFollowRepository.existsByFollowerIdAndCollectionIdAndDeletedAtIsNull(viewerId, collectionId);
            case PRIVATE -> false;
        };
        if (!visible) {
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
        PublicationCollection collection = requireOwned(collectionId, authorId);
        publicationRepository.findByIdAndStatus(publicationId, PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLICATION_NOT_FOUND", "Publicação não encontrada."));
        if (collectionPublicationRepository.existsByCollectionIdAndPublicationId(collectionId, publicationId)) return;
        short nextPosition = (short) (collectionPublicationRepository.findMaxPosition(collectionId) + 1);
        collectionPublicationRepository.save(CollectionPublication.builder()
                .collectionId(collectionId)
                .publicationId(publicationId)
                .position(nextPosition)
                .build());
        // Organizada numa coleção, a publicação sai do balaio solto de "Salvos" —
        // as duas listas mostrando a mesma coisa lida como "salvei duas vezes".
        savedPublicationService.removeAs(publicationId, authorId);
        notifyCollectionFollowers(collection, publicationId, authorId);
    }

    /**
     * "Coisa nova numa coleção que você segue" (docs/telas/12-avisos.html) —
     * avisa quem segue a coleção, exceto quem desativou essa preferência.
     */
    private void notifyCollectionFollowers(PublicationCollection collection, UUID publicationId, UUID authorId) {
        List<CollectionFollow> followerRows = collectionFollowRepository
                .findByCollectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(collection.getId(), Pageable.unpaged())
                .getContent();
        if (followerRows.isEmpty()) return;

        List<UUID> followerIds = followerRows.stream().map(CollectionFollow::getFollowerId).toList();
        List<UserNotification> notifications = userRepository.findAllById(followerIds).stream()
                .filter(follower -> follower.getStatus() == UserStatus.ACTIVE && follower.isNotifyOnCollectionNewItem())
                .map(follower -> UserNotification.builder()
                        .id(UUID.randomUUID())
                        .userId(follower.getId())
                        .type(NEW_ITEM_IN_FOLLOWED_COLLECTION)
                        .actorId(authorId)
                        .publicationId(publicationId)
                        .collectionId(collection.getId())
                        .build())
                .toList();
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
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

    private static final int COVER_IMAGE_COUNT = 1;

    // Capa do cartão de coleção: sempre o último prato adicionado (docs/telas/06-salvos.html
    // — corrigido pra não mostrar mosaico com fotos antigas). Mesma convenção de URL do
    // PublicationResponseFactory ("/images/" + objeto); lista com no máximo 1 item.
    @Transactional(readOnly = true)
    public List<String> coverImageUrls(UUID collectionId) {
        return collectionPublicationRepository
                .findCoverImageObjectNames(collectionId, PageRequest.of(0, COVER_IMAGE_COUNT))
                .stream()
                .map(objectName -> "/images/" + objectName)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Publication> listPublications(UUID collectionId, UUID viewerId, Pageable pageable) {
        findAccessible(collectionId, viewerId);
        Page<CollectionPublication> page = collectionPublicationRepository.findByCollectionIdOrderByPositionAsc(collectionId, pageable);
        List<UUID> ids = page.getContent().stream().map(CollectionPublication::getPublicationId).toList();
        Map<UUID, Publication> byId = publicationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Publication::getId, Function.identity()));
        // A coleção pode ser pública mesmo com uma publicação PRIVATE dentro (o dono pode
        // adicionar a própria publicação PRIVATE à própria coleção pública) — a visibilidade
        // de cada publicação continua valendo dentro da coleção (produto5.md v5 §6.4).
        List<Publication> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .filter(publication -> isAccessibleToViewer(publication, viewerId))
                .toList();
        return new PageImpl<>(ordered, page.getPageable(), page.getTotalElements());
    }

    private boolean isAccessibleToViewer(Publication publication, UUID viewerId) {
        return switch (publication.getVisibility()) {
            case PUBLIC -> true;
            case INTERNAL -> viewerId != null;
            case PRIVATE -> viewerId != null && viewerId.equals(publication.getAuthorId());
        };
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

    /**
     * Link atual para compartilhar, criando um na primeira vez que o dono pedir
     * (impl10.md v10 §13.9). Idempotente — não invalida o link já em uso.
     */
    @Transactional
    public String getOrCreateInviteLink(UUID collectionId, UUID authorId) {
        requireOwned(collectionId, authorId);
        return collectionInviteRepository.findByCollectionIdAndRevokedAtIsNull(collectionId)
                .map(CollectionInvite::getToken)
                .orElseGet(() -> createInvite(collectionId).getToken());
    }

    /**
     * "Gerar novo link": revoga o convite ativo (se existir) e cria outro — o link
     * antigo para de funcionar imediatamente (produto5.md v5 §6.3).
     */
    @Transactional
    public String regenerateInviteLink(UUID collectionId, UUID authorId) {
        requireOwned(collectionId, authorId);
        collectionInviteRepository.findByCollectionIdAndRevokedAtIsNull(collectionId)
                .ifPresent(invite -> {
                    invite.revoke(now());
                    collectionInviteRepository.save(invite);
                });
        return createInvite(collectionId).getToken();
    }

    private CollectionInvite createInvite(UUID collectionId) {
        byte[] randomBytes = new byte[INVITE_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return collectionInviteRepository.save(CollectionInvite.builder()
                .id(UUID.randomUUID())
                .collectionId(collectionId)
                .token(token)
                .build());
    }

    /**
     * Aceitar o convite equivale a seguir a coleção — mesma lista de "quem pode ver"
     * que uma coleção pública já usa (produto5.md v5 §6.3). O próprio dono clicando no
     * próprio link não vira seguidor (ele já tem acesso total).
     */
    @Transactional
    public PublicationCollection acceptInvite(String token, UUID viewerId) {
        requireActiveUser(viewerId);
        CollectionInvite invite = collectionInviteRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Este convite não é válido."));
        PublicationCollection collection = collectionRepository.findByIdAndDeletedAtIsNull(invite.getCollectionId())
                .orElseThrow(() -> new ResourceNotFoundException("COLLECTION_NOT_FOUND", "Coleção não encontrada."));
        if (!collection.getAuthorId().equals(viewerId)) {
            CollectionFollowId id = new CollectionFollowId(viewerId, collection.getId());
            CollectionFollow follow = collectionFollowRepository.findById(id)
                    .orElseGet(() -> CollectionFollow.builder().followerId(viewerId).collectionId(collection.getId()).build());
            follow.reactivate();
            collectionFollowRepository.save(follow);
        }
        return collection;
    }

    /**
     * Convite direto por @usuário (docs/telas/07-colecao.html) — só faz sentido em
     * "Para quem eu escolher"; concede acesso na hora, sem passo de aceite (a mesma
     * CollectionFollow que o link de convite cria ao ser aceito).
     */
    @Transactional
    public User inviteByUsername(UUID collectionId, UUID authorId, String username) {
        PublicationCollection collection = requireOwned(collectionId, authorId);
        if (collection.getVisibility() != CollectionVisibility.SHARED) {
            throw new InvalidOperationException("COLLECTION_NOT_SHARED", "Só é possível convidar pessoas em coleções \"Para quem eu escolher\".");
        }
        User invitee = userRepository.findByUsernameIgnoreCaseAndStatus(username, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
        if (invitee.getId().equals(authorId)) {
            throw new InvalidOperationException("CANNOT_INVITE_SELF", "Você já tem acesso à própria coleção.");
        }
        CollectionFollowId id = new CollectionFollowId(invitee.getId(), collectionId);
        Optional<CollectionFollow> existing = collectionFollowRepository.findById(id);
        boolean alreadyActive = existing.map(f -> f.getDeletedAt() == null).orElse(false);
        CollectionFollow follow = existing
                .orElseGet(() -> CollectionFollow.builder().followerId(invitee.getId()).collectionId(collectionId).build());
        follow.reactivate();
        collectionFollowRepository.save(follow);

        if (!alreadyActive && invitee.isNotifyOnCollectionShared()) {
            notificationRepository.save(UserNotification.builder()
                    .id(UUID.randomUUID())
                    .userId(invitee.getId())
                    .type(COLLECTION_SHARED_WITH_YOU)
                    .actorId(authorId)
                    .collectionId(collectionId)
                    .build());
        }
        return invitee;
    }

    /**
     * Tira o acesso de uma pessoa específica, sem afetar as outras nem o link de
     * convite em si (docs/telas/07-colecao.html, botão "Remover" por pessoa).
     */
    @Transactional
    public void removeInvitee(UUID collectionId, UUID authorId, UUID inviteeId) {
        requireOwned(collectionId, authorId);
        collectionFollowRepository.findById(new CollectionFollowId(inviteeId, collectionId))
                .ifPresent(follow -> {
                    follow.remove(now());
                    collectionFollowRepository.save(follow);
                });
    }

    /**
     * "Quem já tem acesso", para o modal de compartilhar (impl10.md v10 §13.9) — só o dono
     * pode consultar.
     */
    @Transactional(readOnly = true)
    public Page<User> listInvitees(UUID collectionId, UUID authorId, Pageable pageable) {
        requireOwned(collectionId, authorId);
        Page<CollectionFollow> page = collectionFollowRepository.findByCollectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(collectionId, pageable);
        List<UUID> ids = page.getContent().stream().map(CollectionFollow::getFollowerId).toList();
        Map<UUID, User> byId = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<User> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, page.getPageable(), page.getTotalElements());
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
