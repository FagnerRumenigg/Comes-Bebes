<script setup lang="ts">
import { computed } from 'vue'

import type { PublicationResponseVisibility } from '@/api/generated/models'

import VisibilityBadge from './VisibilityBadge.vue'

const props = defineProps<{
  publicationId: string
  authorDisplayName: string
  authorUsername: string
  publishedAt: string
  photoTakenAt?: string | null
  visibility: PublicationResponseVisibility
}>()

const publishedLabel = computed(() =>
  new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(props.publishedAt)),
)

const photoTakenAtLabel = computed(() =>
  props.photoTakenAt
    ? new Intl.DateTimeFormat('pt-BR', {
        day: 'numeric',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(props.photoTakenAt))
    : null,
)
</script>

<template>
  <header class="publication-header">
    <div class="publication-header__identity">
      <RouterLink class="publication-header__name" :to="`/u/${authorUsername}`">
        {{ authorDisplayName }}
      </RouterLink>
      <span class="publication-header__meta">
        @{{ authorUsername }}
        <span aria-hidden="true">·</span>
        <time :datetime="publishedAt">{{ publishedLabel }}</time>
      </span>
      <span v-if="photoTakenAtLabel" class="publication-header__photo-meta">
        Foto tirada em <time :datetime="photoTakenAt!">{{ photoTakenAtLabel }}</time>
      </span>
    </div>
    <div class="publication-header__actions">
      <VisibilityBadge :visibility="visibility" />
      <RouterLink
        class="publication-header__details"
        :to="`/publicacoes/${publicationId}`"
        aria-label="Ver publicação"
        title="Ver publicação"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M5 19 19 5m-9 0h9v9" />
        </svg>
      </RouterLink>
    </div>
  </header>
</template>

<style scoped>
.publication-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
}

.publication-header__identity {
  display: grid;
  gap: var(--space-1);
  min-width: 0;
}

.publication-header__name {
  overflow: hidden;
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publication-header__name:hover {
  color: var(--color-primary);
}

.publication-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.publication-header__photo-meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.publication-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.publication-header__details {
  display: grid;
  width: 2rem;
  height: 2rem;
  flex: none;
  place-items: center;
  color: var(--color-text-secondary);
  border-radius: var(--radius-pill);
}

.publication-header__details:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.publication-header__details svg {
  width: 1.125rem;
  fill: none;
  stroke: currentcolor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

@media (max-width: 30rem) {
  .publication-header {
    padding: var(--space-4);
  }
}
</style>
