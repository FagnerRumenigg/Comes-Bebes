<script setup lang="ts">
import { computed } from 'vue'

import type { PublicationResponse } from '@/api/generated/models'
import { formatRelativeTime } from '@/utils/relativeTime'
import { resolveImageUrl } from '@/utils/resolveImageUrl'

const props = defineProps<{
  publication: PublicationResponse
  /** Mostra "de {autor}" — usado quando o grid mistura publicações de várias pessoas. */
  showAuthor?: boolean
}>()

const caption = computed(() => props.publication.title ?? props.publication.authorDisplayName)
</script>

<template>
  <RouterLink class="publication-tile" :to="`/publicacoes/${publication.id}`">
    <span class="publication-tile__image-wrapper">
      <img
        v-if="publication.imageUrl"
        class="publication-tile__image"
        :src="resolveImageUrl(publication.imageUrl)"
        :alt="caption"
        loading="lazy"
      />
    </span>
    <h3 class="publication-tile__caption">{{ caption }}</h3>
    <span class="publication-tile__meta">
      <template v-if="showAuthor">de {{ publication.authorDisplayName }}</template>
      <template v-else>{{ formatRelativeTime(publication.publishedAt) }}</template>
    </span>
  </RouterLink>
</template>

<style scoped>
.publication-tile {
  display: block;
  color: inherit;
  text-decoration: none;
}

.publication-tile__image-wrapper {
  display: block;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.publication-tile__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-moderate) var(--ease-standard);
}

.publication-tile:hover .publication-tile__image {
  transform: scale(1.03);
}

.publication-tile__caption {
  display: block;
  overflow: hidden;
  margin: var(--space-2) 0 0;
  color: var(--color-text);
  font-family: var(--font-interface);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  letter-spacing: normal;
  text-overflow: ellipsis;
  text-wrap: initial;
  white-space: nowrap;
}

.publication-tile__meta {
  display: block;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}
</style>
