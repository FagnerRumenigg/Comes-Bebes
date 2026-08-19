package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.Publication;
import org.application.model.PublicationView;
import org.application.repository.PublicationRepository;
import org.application.repository.PublicationViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationViewService {

    private final PublicationViewRepository publicationViewRepository;
    private final PublicationRepository publicationRepository;

    @Transactional
    public void markViewed(UUID userId, List<UUID> publicationIds) {
        List<UUID> distinctIds = publicationIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            return;
        }

        Set<UUID> existingPublicationIds = publicationRepository.findAllById(distinctIds).stream()
                .map(Publication::getId)
                .collect(Collectors.toSet());
        Set<UUID> alreadyViewedIds = publicationViewRepository.findByUserIdAndPublicationIdIn(userId, distinctIds).stream()
                .map(PublicationView::getPublicationId)
                .collect(Collectors.toSet());

        List<PublicationView> newViews = distinctIds.stream()
                .filter(existingPublicationIds::contains)
                .filter(id -> !alreadyViewedIds.contains(id))
                .map(id -> PublicationView.builder().userId(userId).publicationId(id).build())
                .toList();

        if (!newViews.isEmpty()) {
            publicationViewRepository.saveAll(newViews);
        }
    }
}
