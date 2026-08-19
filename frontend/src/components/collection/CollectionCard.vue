<script setup lang="ts">
import type { CollectionResponse } from '@/api/generated/models'

defineProps<{ collection: CollectionResponse }>()
</script>

<template>
  <RouterLink class="collection-card" :to="`/colecoes/${collection.id}`">
    <div class="collection-card__heading">
      <h3>{{ collection.name }}</h3>
      <span v-if="collection.visibility === 'PRIVATE'" class="collection-card__badge">Privada</span>
    </div>
    <p v-if="collection.description">{{ collection.description }}</p>
    <p class="collection-card__meta">
      {{ collection.publicationsCount }}
      {{ collection.publicationsCount === 1 ? 'publicação' : 'publicações' }} ·
      {{ collection.followersCount }}
      {{ collection.followersCount === 1 ? 'seguidor' : 'seguidores' }}
    </p>
  </RouterLink>
</template>

<style scoped>
.collection-card {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  color: inherit;
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: border-color var(--duration-fast) var(--ease-standard);
}

.collection-card:hover {
  border-color: var(--color-primary);
}

.collection-card__heading {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.collection-card__heading h3 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.collection-card__badge {
  padding: var(--space-1) var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.collection-card p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.collection-card__meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}
</style>
