package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.user.response.NotificationResponse;
import org.application.dto.PageResponse;
import org.application.model.Publication;
import org.application.model.PublicationCollection;
import org.application.model.User;
import org.application.model.UserNotification;
import org.application.repository.PublicationCollectionRepository;
import org.application.repository.PublicationRepository;
import org.application.repository.UserRepository;
import org.application.util.DateTimeConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta {@link NotificationResponse} em lote (docs/telas/12-avisos.html precisa
 * de nome do autor, título/imagem da publicação e nome da coleção no texto de
 * cada aviso) — busca os relacionados de uma vez, no mesmo padrão de
 * FollowService#mapToUsers, em vez de N+1 por item da página.
 */
@Service
@RequiredArgsConstructor
public class NotificationResponseFactory {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final PublicationCollectionRepository collectionRepository;

    public PageResponse<NotificationResponse> of(Page<UserNotification> page, ZoneId zoneId) {
        Map<UUID, User> usersById = userRepository.findAllById(distinct(page, UserNotification::getActorId))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Publication> publicationsById = publicationRepository.findAllById(distinct(page, UserNotification::getPublicationId))
                .stream().collect(Collectors.toMap(Publication::getId, Function.identity()));
        Map<UUID, PublicationCollection> collectionsById = collectionRepository.findAllById(distinct(page, UserNotification::getCollectionId))
                .stream().collect(Collectors.toMap(PublicationCollection::getId, Function.identity()));

        return PageResponse.of(page, item -> {
            User actor = usersById.get(item.getActorId());
            Publication publication = publicationsById.get(item.getPublicationId());
            PublicationCollection collection = collectionsById.get(item.getCollectionId());
            return NotificationResponse.builder()
                    .id(item.getId())
                    .type(item.getType())
                    .moderationCaseId(item.getModerationCaseId())
                    .publicationId(item.getPublicationId())
                    .collectionId(item.getCollectionId())
                    .actorId(item.getActorId())
                    .actorDisplayName(actor == null ? null : actor.getDisplayName())
                    .publicationTitle(publication == null ? null : publication.getTitle())
                    .publicationImageUrl(publication == null ? null : "/images/" + publication.getGcsObjectName())
                    .collectionName(collection == null ? null : collection.getName())
                    .createdAt(DateTimeConverter.toApplicationTime(item.getCreatedAt(), zoneId))
                    .readAt(DateTimeConverter.toApplicationTime(item.getReadAt(), zoneId))
                    .build();
        });
    }

    private java.util.List<UUID> distinct(Page<UserNotification> page, Function<UserNotification, UUID> idExtractor) {
        return page.getContent().stream().map(idExtractor).filter(java.util.Objects::nonNull).distinct().toList();
    }
}
