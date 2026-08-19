package org.application.repository;

import org.application.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlugAndMergedIntoTagIdIsNull(String slug);
    List<Tag> findBySlugInAndMergedIntoTagIdIsNull(List<String> slugs);
    List<Tag> findTop10BySlugStartingWithAndMergedIntoTagIdIsNullOrderByOfficialDescNameAsc(String slugPrefix);
}
