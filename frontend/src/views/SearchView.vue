<script setup lang="ts">
import { computed, ref } from 'vue'

import { useSearch } from '@/api/generated/publications/publications'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'

const title = ref('')
const ingredient = ref('')
const submitted = ref(false)
const hasSearchTerm = computed(() => Boolean(title.value.trim() || ingredient.value.trim()))
const params = computed(() => ({
  title: title.value.trim() || undefined,
  ingredient: ingredient.value.trim() || undefined,
  page: 1,
  size: 20,
}))
const searchQuery = useSearch(params, {
  query: {
    enabled: computed(() => submitted.value && hasSearchTerm.value),
  },
})

function submit(): void {
  if (!hasSearchTerm.value) {
    return
  }

  submitted.value = true
}
function clear(): void {
  title.value = ''
  ingredient.value = ''
  submitted.value = false
}
</script>

<template>
  <section class="search-view">
    <header class="search-view__header">
      <p class="search-view__eyebrow">Explorar</p>
      <h1>Buscar publicações</h1>
      <p>Encontre pratos e receitas por título ou ingrediente.</p>
    </header>
    <form class="search-view__form" @submit.prevent="submit">
      <BaseInput v-model="title" label="Título" placeholder="Ex.: bolo de cenoura" />
      <BaseInput v-model="ingredient" label="Ingrediente" placeholder="Ex.: chocolate" />
      <div class="search-view__actions">
        <BaseButton type="submit" :disabled="!hasSearchTerm">Buscar</BaseButton
        ><BaseButton type="button" variant="secondary" @click="clear">Limpar</BaseButton>
      </div>
    </form>
    <div v-if="!submitted" class="search-view__state">
      Informe um título ou ingrediente para começar.
    </div>
    <div v-else-if="searchQuery.isPending.value" class="search-view__state">Buscando...</div>
    <div v-else-if="searchQuery.isError.value" class="search-view__state" role="alert">
      Não foi possível realizar a busca.
    </div>
    <div v-else-if="!searchQuery.data.value?.content.length" class="search-view__state">
      Nenhuma publicação encontrada.
    </div>
    <div v-else class="search-view__list">
      <PublicationCard
        v-for="publication in searchQuery.data.value.content"
        :key="publication.id"
        :publication="publication"
      />
    </div>
  </section>
</template>

<style scoped>
.search-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.search-view__header {
  margin-block-end: var(--space-8);
}
.search-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.search-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.search-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.search-view__form {
  display: grid;
  gap: var(--space-4);
  margin-block-end: var(--space-8);
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.search-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}
.search-view__list {
  display: grid;
  gap: var(--space-8);
}
.search-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
