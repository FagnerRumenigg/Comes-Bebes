package org.application.service;

import org.application.model.PublicationTag;
import org.application.model.Tag;
import org.application.repository.PublicationTagRepository;
import org.application.repository.TagRepository;
import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private PublicationTagRepository publicationTagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void shouldSlugifyRemovingAccentsAndSpecialCharacters() {
        assertThat(TagService.slugify("Açaí")).isEqualTo("acai");
        assertThat(TagService.slugify("  Pão de Queijo!! ")).isEqualTo("pao-de-queijo");
        assertThat(TagService.slugify("Café com Leite")).isEqualTo("cafe-com-leite");
    }

    @Test
    void shouldRejectMoreThanFiveTags() {
        List<String> sixTags = List.of("a", "b", "c", "d", "e", "f");

        assertThatThrownBy(() -> tagService.resolveOrCreate(sixTags, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasFieldOrPropertyWithValue("code", "TOO_MANY_TAGS");

        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldReuseExistingTagBySlugInsteadOfCreatingDuplicate() {
        UUID creatorId = UUID.randomUUID();
        Tag existing = Tag.builder().id(UUID.randomUUID()).name("Açaí").slug("acai").official(true).build();
        when(tagRepository.findBySlugInAndMergedIntoTagIdIsNull(List.of("acai")))
                .thenReturn(List.of(existing));

        List<Tag> resolved = tagService.resolveOrCreate(List.of("açaí"), creatorId);

        assertThat(resolved).containsExactly(existing);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewNonOfficialTagWhenSlugDoesNotExist() {
        UUID creatorId = UUID.randomUUID();
        when(tagRepository.findBySlugInAndMergedIntoTagIdIsNull(List.of("vegano")))
                .thenReturn(List.of());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Tag> resolved = tagService.resolveOrCreate(List.of("Vegano"), creatorId);

        assertThat(resolved).hasSize(1);
        Tag created = resolved.get(0);
        assertThat(created.getSlug()).isEqualTo("vegano");
        assertThat(created.getName()).isEqualTo("Vegano");
        assertThat(created.isOfficial()).isFalse();
        assertThat(created.getCreatedBy()).isEqualTo(creatorId);
    }

    @Test
    void shouldDeduplicateEquivalentTagsBySlug() {
        when(tagRepository.findBySlugInAndMergedIntoTagIdIsNull(List.of("acai")))
                .thenReturn(List.of());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Tag> resolved = tagService.resolveOrCreate(List.of("Açaí", "açaí", "  ACAÍ  "), UUID.randomUUID());

        assertThat(resolved).hasSize(1);
        verify(tagRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void shouldReplaceExistingAssociationsWhenAttachingToPublication() {
        UUID publicationId = UUID.randomUUID();
        Tag tag = Tag.builder().id(UUID.randomUUID()).name("Vegano").slug("vegano").build();

        tagService.attachToPublication(publicationId, List.of(tag));

        verify(publicationTagRepository).deleteByPublicationId(publicationId);
        ArgumentCaptor<List<PublicationTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(publicationTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPublicationId()).isEqualTo(publicationId);
        assertThat(captor.getValue().get(0).getTagId()).isEqualTo(tag.getId());
    }

    @Test
    void shouldOnlyDeleteWhenAttachingEmptyTagList() {
        UUID publicationId = UUID.randomUUID();

        tagService.attachToPublication(publicationId, List.of());

        verify(publicationTagRepository).deleteByPublicationId(publicationId);
        verify(publicationTagRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldSearchByNormalizedPrefixOrderingOfficialFirst() {
        Tag official = Tag.builder().id(UUID.randomUUID()).name("Açaí").slug("acai").official(true).build();
        when(tagRepository.findTop10BySlugStartingWithAndMergedIntoTagIdIsNullOrderByOfficialDescNameAsc("aca"))
                .thenReturn(List.of(official));

        List<Tag> result = tagService.search("Aça", 10);

        assertThat(result).containsExactly(official);
    }

    @Test
    void shouldReturnEmptyListWhenSearchQueryNormalizesToBlank() {
        List<Tag> result = tagService.search("!!!", 10);

        assertThat(result).isEmpty();
        verify(tagRepository, never())
                .findTop10BySlugStartingWithAndMergedIntoTagIdIsNullOrderByOfficialDescNameAsc(any());
    }

    @Test
    void shouldFindTagsByPublicationIdPreservingAssociationOrder() {
        UUID publicationId = UUID.randomUUID();
        UUID firstTagId = UUID.randomUUID();
        UUID secondTagId = UUID.randomUUID();
        Tag first = Tag.builder().id(firstTagId).name("Vegano").slug("vegano").build();
        Tag second = Tag.builder().id(secondTagId).name("Doce").slug("doce").build();

        when(publicationTagRepository.findByPublicationIdOrderByCreatedAtAsc(publicationId)).thenReturn(List.of(
                PublicationTag.builder().publicationId(publicationId).tagId(firstTagId).build(),
                PublicationTag.builder().publicationId(publicationId).tagId(secondTagId).build()));
        when(tagRepository.findAllById(List.of(firstTagId, secondTagId))).thenReturn(List.of(second, first));

        List<Tag> result = tagService.findByPublicationId(publicationId);

        assertThat(result).containsExactly(first, second);
    }
}
