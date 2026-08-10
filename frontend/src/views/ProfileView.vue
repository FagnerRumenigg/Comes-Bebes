<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useFindByUsername } from '@/api/generated/profiles/profiles'
import { useGetUserPublications } from '@/api/generated/users/users'
import PublicationCard from '@/components/publication/PublicationCard.vue'

const route = useRoute()
const username = computed(() => String(route.params.username ?? ''))
const profileQuery = useFindByUsername(username)
const userId = computed(() => profileQuery.data.value?.id ?? '')
const publicationsQuery = useGetUserPublications(userId, { page: 1, size: 20 })
</script>

<template>
  <section class="profile-view">
    <div v-if="profileQuery.isPending.value" class="profile-view__state">Carregando perfil...</div>
    <div v-else-if="profileQuery.isError.value" class="profile-view__state" role="alert">
      Perfil não encontrado.
    </div>
    <template v-else-if="profileQuery.data.value">
      <header class="profile-view__header">
        <p class="profile-view__eyebrow">Perfil público</p>
        <h1>{{ profileQuery.data.value.displayName }}</h1>
        <p>@{{ profileQuery.data.value.username }}</p>
      </header>
      <div v-if="publicationsQuery.isPending.value" class="profile-view__state">
        Carregando publicações...
      </div>
      <div v-else-if="!publicationsQuery.data.value?.content.length" class="profile-view__state">
        Este perfil ainda não tem publicações visíveis.
      </div>
      <div v-else class="profile-view__list">
        <PublicationCard
          v-for="publication in publicationsQuery.data.value.content"
          :key="publication.id"
          :publication="publication"
        />
      </div>
    </template>
  </section>
</template>

<style scoped>
.profile-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.profile-view__header {
  margin-block-end: var(--space-10);
}
.profile-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.profile-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.profile-view__header > p:last-child {
  margin-block-start: var(--space-2);
  color: var(--color-text-secondary);
}
.profile-view__list {
  display: grid;
  gap: var(--space-8);
}
.profile-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
