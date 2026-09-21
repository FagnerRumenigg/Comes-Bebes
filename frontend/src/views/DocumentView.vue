<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useDocument } from '@/features/documents/documents'

const route = useRoute()
const slug = computed(() => route.meta.documentSlug ?? 'FAQ')
const documentQuery = useDocument(slug)

const paragraphs = computed(() => (documentQuery.data.value?.body ?? '').split(/\n{2,}/).filter(Boolean))
const isSectionHeading = (paragraph: string) => /^\d+\./.test(paragraph)
</script>

<template>
  <section class="document-view">
    <div v-if="documentQuery.isPending.value" class="document-view__state">Carregando...</div>
    <div v-else-if="documentQuery.isError.value" class="document-view__state" role="alert">
      Não foi possível carregar este documento.
    </div>
    <template v-else-if="documentQuery.data.value">
      <header class="document-view__header">
        <p class="document-view__eyebrow">Documento vigente</p>
        <h1>{{ documentQuery.data.value.title }}</h1>
        <p class="document-view__updated">
          Atualizado em {{ new Date(documentQuery.data.value.updatedAt).toLocaleDateString('pt-BR') }}
        </p>
      </header>
      <div class="document-view__content">
        <p
          v-for="(paragraph, index) in paragraphs"
          :key="index"
          :class="{ 'document-view__section': isSectionHeading(paragraph) }"
        >{{ paragraph }}</p>
      </div>
    </template>
  </section>
</template>

<style scoped>
.document-view {
  max-width: var(--content-narrow);
  margin-inline: auto;
  padding-block: var(--space-6) var(--space-10);
}

.document-view__header {
  padding-block-end: var(--space-6);
  border-block-end: 1px solid var(--color-border);
}

.document-view__eyebrow {
  margin: 0 0 var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.document-view h1 {
  margin: 0;
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  font-size: clamp(2rem, 5vw, 2.5rem);
}

.document-view__updated {
  margin: var(--space-3) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.document-view__content {
  padding-block-start: var(--space-6);
}

.document-view p {
  margin: 0 0 var(--space-4);
  color: var(--color-text);
  line-height: var(--line-height-body);
  white-space: pre-line;
}

.document-view__content .document-view__section {
  margin-block-start: var(--space-8);
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.document-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
