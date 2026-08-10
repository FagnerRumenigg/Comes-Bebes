<script setup lang="ts">
import { computed } from 'vue'

import {
  getGetPublicationByIdQueryOptions,
  useSaved,
} from '@/api/generated/publications/publications'
import { useQueries } from '@tanstack/vue-query'
import PublicationCard from '@/components/publication/PublicationCard.vue'

const savedQuery = useSaved({ page: 1, size: 20 })
const details = useQueries({
  queries: computed(() =>
    (savedQuery.data.value?.content ?? []).map((item) =>
      getGetPublicationByIdQueryOptions(item.publicationId),
    ),
  ),
})
const publications = computed(() =>
  details.value
    .map((query) => query.data)
    .filter((publication): publication is NonNullable<typeof publication> => Boolean(publication))
    .map((publication) => ({ ...publication, saved: true })),
)
const detailsPending = computed(() => details.value.some((query) => query.isPending))
const detailsError = computed(() => details.value.some((query) => query.isError))
</script>

<template>
  <section class="saved-view">
    <header class="saved-view__header">
      <p class="saved-view__eyebrow">Sua coleção</p>
      <h1>Salvos</h1>
      <p>Publicações guardadas para voltar quando quiser.</p>
    </header>
    <div v-if="savedQuery.isPending.value" class="saved-view__state">Carregando salvos...</div>
    <div v-else-if="savedQuery.isError.value" class="saved-view__state" role="alert">
      Não foi possível carregar seus salvos.
    </div>
    <div v-else-if="!savedQuery.data.value?.content.length" class="saved-view__state">
      Você ainda não salvou nenhuma publicação.
    </div>
    <div v-else-if="detailsPending" class="saved-view__state">Carregando publicações...</div>
    <div v-else-if="detailsError" class="saved-view__state" role="alert">
      Não foi possível carregar uma ou mais publicações salvas.
    </div>
    <div v-else class="saved-view__list">
      <PublicationCard
        v-for="publication in publications"
        :key="publication.id"
        :publication="publication"
      />
    </div>
  </section>
</template>

<style scoped>
.saved-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.saved-view__header {
  margin-block-end: var(--space-8);
}
.saved-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.saved-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.saved-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.saved-view__list {
  display: grid;
  gap: var(--space-8);
}
.saved-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
