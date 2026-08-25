<script setup lang="ts">
import { computed } from 'vue'

import type { PublicationResponseVisibility } from '@/api/generated/models'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { useAuthStore } from '@/stores/auth.store'

import VisibilityBadge from './VisibilityBadge.vue'

const props = defineProps<{
  publicationId: string
  authorId: string
  authorDisplayName: string
  authorUsername: string
  publishedAt: string
  photoTakenAt?: string | null
  visibility: PublicationResponseVisibility
}>()

const authStore = useAuthStore()
const isOwn = computed(() => authStore.identity?.userId === props.authorId)

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
    <RouterLink class="publication-header__avatar" :to="`/u/${authorUsername}`" tabindex="-1" aria-hidden="true">
      <BaseAvatar :name="authorDisplayName" size="small" />
    </RouterLink>
    <div class="publication-header__identity">
      <span class="publication-header__name-row">
        <RouterLink class="publication-header__name" :to="`/u/${authorUsername}`">
          {{ authorDisplayName }}
        </RouterLink>
        <span v-if="isOwn" class="publication-header__own-badge">Sua publicação</span>
      </span>
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
        <AppIcon name="chevron-right" :size="18" :stroke-width="2" />
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

.publication-header__avatar {
  flex: none;
}

.publication-header__identity {
  display: grid;
  flex: 1;
  gap: var(--space-1);
  min-width: 0;
}

.publication-header__name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.publication-header__name {
  overflow: hidden;
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publication-header__own-badge {
  padding: 0.125rem var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
  border-radius: var(--radius-pill);
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

@media (max-width: 30rem) {
  .publication-header {
    padding: var(--space-4);
  }
}
</style>
