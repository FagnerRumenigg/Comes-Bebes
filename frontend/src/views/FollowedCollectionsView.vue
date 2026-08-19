<script setup lang="ts">
import { useGetFollowedCollections } from '@/api/generated/collections/collections'
import CollectionCard from '@/components/collection/CollectionCard.vue'

const followedQuery = useGetFollowedCollections({ page: 1, size: 20 })
</script>

<template>
  <section class="followed-collections-view">
    <header class="followed-collections-view__header">
      <p class="followed-collections-view__eyebrow">Suas coleções seguidas</p>
      <h1>Coleções que você segue</h1>
      <p>Listas curadas de outras pessoas, reunidas em um só lugar.</p>
    </header>
    <div v-if="followedQuery.isPending.value" class="followed-collections-view__state">
      Carregando coleções...
    </div>
    <div v-else-if="followedQuery.isError.value" class="followed-collections-view__state" role="alert">
      Não foi possível carregar as coleções seguidas.
    </div>
    <div
      v-else-if="!followedQuery.data.value?.content.length"
      class="followed-collections-view__state"
    >
      Você ainda não segue nenhuma coleção.
    </div>
    <div v-else class="followed-collections-view__list">
      <CollectionCard
        v-for="collection in followedQuery.data.value.content"
        :key="collection.id"
        :collection="collection"
      />
    </div>
  </section>
</template>

<style scoped>
.followed-collections-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.followed-collections-view__header {
  margin-block-end: var(--space-8);
}
.followed-collections-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.followed-collections-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.followed-collections-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.followed-collections-view__list {
  display: grid;
  gap: var(--space-4);
}
.followed-collections-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
