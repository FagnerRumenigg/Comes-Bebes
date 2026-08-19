package org.application.service;

import lombok.RequiredArgsConstructor;
import org.application.model.PublicationTag;
import org.application.model.Tag;
import org.application.repository.PublicationTagRepository;
import org.application.repository.TagRepository;
import org.application.service.exception.InvalidOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TagService {
    private static final int MAX_TAGS_PER_PUBLICATION = 5;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

    private final TagRepository tagRepository;
    private final PublicationTagRepository publicationTagRepository;

    /**
     * Sem diferenciar acento nem maiúscula/minúscula, para o autocomplete e a busca
     * tratarem "Açaí" e "acai" como a mesma tag.
     */
    public static String slugify(String rawName) {
        String lowercase = rawName.trim().toLowerCase(Locale.ROOT);
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(lowercase, Normalizer.Form.NFD)).replaceAll("");
        String slug = NON_SLUG_CHARS.matcher(withoutDiacritics).replaceAll("-");
        return EDGE_HYPHENS.matcher(slug).replaceAll("");
    }

    @Transactional
    public List<Tag> resolveOrCreate(List<String> rawNames, UUID creatorId) {
        if (rawNames == null || rawNames.isEmpty()) return List.of();
        if (rawNames.size() > MAX_TAGS_PER_PUBLICATION) {
            throw new InvalidOperationException("TOO_MANY_TAGS",
                    "No máximo " + MAX_TAGS_PER_PUBLICATION + " tags por publicação.");
        }

        Map<String, String> nameBySlug = new LinkedHashMap<>();
        for (String rawName : rawNames) {
            if (rawName == null || rawName.isBlank()) continue;
            String slug = slugify(rawName);
            if (slug.isBlank()) continue;
            nameBySlug.putIfAbsent(slug, rawName.trim());
        }
        if (nameBySlug.isEmpty()) return List.of();

        List<String> slugs = List.copyOf(nameBySlug.keySet());
        Map<String, Tag> existingBySlug = new LinkedHashMap<>();
        tagRepository.findBySlugInAndMergedIntoTagIdIsNull(slugs)
                .forEach(tag -> existingBySlug.put(tag.getSlug(), tag));

        List<Tag> resolved = new ArrayList<>();
        for (String slug : slugs) {
            Tag tag = existingBySlug.get(slug);
            if (tag == null) {
                tag = tagRepository.save(Tag.builder()
                        .id(UUID.randomUUID())
                        .name(nameBySlug.get(slug))
                        .slug(slug)
                        .official(false)
                        .createdBy(creatorId)
                        .build());
            }
            resolved.add(tag);
        }
        return resolved;
    }

    @Transactional
    public void attachToPublication(UUID publicationId, List<Tag> tags) {
        publicationTagRepository.deleteByPublicationId(publicationId);
        if (tags.isEmpty()) return;
        publicationTagRepository.saveAll(tags.stream()
                .map(tag -> PublicationTag.builder().publicationId(publicationId).tagId(tag.getId()).build())
                .toList());
    }

    @Transactional(readOnly = true)
    public List<Tag> findByPublicationId(UUID publicationId) {
        List<UUID> tagIds = publicationTagRepository.findByPublicationIdOrderByCreatedAtAsc(publicationId).stream()
                .map(PublicationTag::getTagId)
                .toList();
        if (tagIds.isEmpty()) return List.of();
        Map<UUID, Tag> tagsById = new LinkedHashMap<>();
        tagRepository.findAllById(tagIds).forEach(tag -> tagsById.put(tag.getId(), tag));
        return tagIds.stream().map(tagsById::get).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public List<Tag> search(String query, int limit) {
        String slugPrefix = slugify(query == null ? "" : query);
        if (slugPrefix.isBlank()) return List.of();
        return tagRepository
                .findTop10BySlugStartingWithAndMergedIntoTagIdIsNullOrderByOfficialDescNameAsc(slugPrefix)
                .stream()
                .limit(limit)
                .toList();
    }
}
