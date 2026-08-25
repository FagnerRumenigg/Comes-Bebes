<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useDocument } from '@/features/documents/documents'

const route = useRoute()
const slug = computed(() => route.meta.documentSlug ?? 'FAQ')
const documentQuery = useDocument(slug)

const paragraphs = computed(() => (documentQuery.data.value?.body ?? '').split(/\n{2,}/).filter(Boolean))
</script>

<template>
  <section class="document-view">
    <div v-if="documentQuery.isPending.value" class="document-view__state">Carregando...</div>
    <div v-else-if="documentQuery.isError.value" class="document-view__state" role="alert">
      Não foi possível carregar este documento.
    </div>
    <template v-else-if="documentQuery.data.value">
      <h1>{{ documentQuery.data.value.title }}</h1>
      <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
    </template>
  </section>
</template>

<style scoped>
.document-view {
  max-width: var(--content-narrow);
  margin-inline: auto;
}

.document-view h1 {
  margin: 0 0 var(--space-6);
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  font-size: clamp(2rem, 5vw, 2.5rem);
}

.document-view p {
  margin: 0 0 var(--space-4);
  color: var(--color-text);
  line-height: var(--line-height-body);
  white-space: pre-line;
}

.document-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
