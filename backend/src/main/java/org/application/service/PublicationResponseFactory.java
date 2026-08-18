package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.controller.publication.response.PublicationResponse;
import org.application.controller.tag.response.TagResponse;
import org.application.model.Publication;
import org.application.repository.PublicationOriginRepository;
import org.application.repository.PublicationReactionRepository;
import org.application.repository.ReactionTypeRepository;
import org.application.repository.SavedPublicationRepository;
import org.application.repository.UserRepository;
import org.application.repository.ReportRepository;
import org.application.controller.publication.response.RecipeResponse;
import org.application.model.ReactionCode;
import org.application.service.exception.ResourceNotFoundException;
import org.application.util.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationResponseFactory {
    private static final Logger log = LoggerFactory.getLogger(PublicationResponseFactory.class);

    private final UserRepository userRepository;
    private final PublicationReactionRepository reactionRepository;
    private final ReactionTypeRepository reactionTypeRepository;
    private final SavedPublicationRepository savedPublicationRepository;
    private final PublicationOriginRepository publicationOriginRepository;
    private final ReportRepository reportRepository;
    private final RecipeService recipeService;
    private final TagService tagService;

    public PublicationResponse of(Publication publication, ZoneId zoneId, UUID viewerId) {
        var author = userRepository.findById(publication.getAuthorId());
        var reactions = reactionRepository.findByPublicationIdAndDeletedAtIsNull(publication.getId());
        var types = reactionTypeRepository.findAllById(reactions.stream()
                .map(reaction -> reaction.getReactionTypeId()).toList()).stream()
                .collect(Collectors.toMap(type -> type.getId(), type -> type.getCode()));
        Map<ReactionCode, Long> totals = new LinkedHashMap<>();
        reactions.forEach(reaction -> {
            String code = types.get(reaction.getReactionTypeId());
            if (code != null) {
                totals.merge(ReactionCode.valueOf(code), 1L, Long::sum);
            }
        });
        List<ReactionCode> selected = reactions.stream()
                .filter(reaction -> reaction.getUserId().equals(viewerId))
                .map(reaction -> types.get(reaction.getReactionTypeId()))
                .filter(java.util.Objects::nonNull)
                .map(ReactionCode::valueOf)
                .toList();
        var origin = publicationOriginRepository.findById(publication.getId()).orElse(null);
        RecipeResponse preview = null;
        if (publication.getType() == org.application.model.PublicationType.RECIPE
                || publication.getType() == org.application.model.PublicationType.MY_VERSION) {
            try {
                preview = RecipeResponse.of(recipeService.findDetails(publication.getId()));
            } catch (ResourceNotFoundException exception) {
                preview = null;
            } catch (RuntimeException exception) {
                log.warn("event=recipe_preview_build_failed publicationId={}", publication.getId(), exception);
                preview = null;
            }
        }
        return PublicationResponse.builder()
                .id(publication.getId())
                .authorId(publication.getAuthorId())
                .type(publication.getType())
                .visibility(publication.getVisibility())
                .title(publication.getTitle())
                .description(publication.getDescription())
                .status(publication.getStatus())
                .publishedAt(DateTimeConverter.toApplicationTime(publication.getPublishedAt(), zoneId))
                .photoTakenAt(DateTimeConverter.toApplicationTime(publication.getPhotoTakenAt(), zoneId))
                .imageUrl("/images/" + publication.getGcsObjectName())
                .authorUsername(author.map(org.application.model.User::getUsername).orElse(null))
                .authorDisplayName(author.map(org.application.model.User::getDisplayName).orElse(null))
                .showReactionCounts(author.map(org.application.model.User::isShowReactionCounts).orElse(true))
                .reactionTotals(totals)
                .selectedReactions(selected)
                .saved(viewerId != null && savedPublicationRepository.existsByUserIdAndPublicationIdAndDeletedAtIsNull(viewerId, publication.getId()))
                .versionsCount(publicationOriginRepository.countBySourceRecipeId(publication.getId()))
                .originalPublicationId(origin == null ? null : origin.getSourceRecipeId())
                .reportedByCurrentUser(viewerId != null && reportRepository.existsByPublicationIdAndReporterIdAndResolution(publication.getId(), viewerId, "PENDING"))
                .recipePreview(preview)
                .editedByAdmin(publication.getEditedByAdminId() != null)
                .tags(tagService.findByPublicationId(publication.getId()).stream().map(TagResponse::of).toList())
                .build();
    }
}
