<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import { useDraftsList } from '@/features/publications/useDraftsList'
import type { PublicationDraft } from '@/features/publications/drafts'

const router = useRouter()
const { drafts, loading, refresh, remove } = useDraftsList()

function continueDraft(id: string): void {
  void router.push(`/publicar/rascunho/${id}`)
}

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

function draftTitle(draft: PublicationDraft): string {
  if (draft.mode === 'MY_VERSION') return draft.titleSuffix.trim() || 'Minha versão sem título'
  return draft.title.trim() || 'Rascunho sem título'
}

function draftTypeLabel(draft: PublicationDraft): string {
  if (draft.mode === 'MY_VERSION') return 'Minha versão'
  return draft.type === 'RECIPE' ? 'Receita' : 'Prato'
}

const previews = computed(() => {
  const map = new Map<string, string>()
  for (const draft of drafts.value) {
    if (draft.image) map.set(draft.id, URL.createObjectURL(draft.image))
  }
  return map
})

function revokePreviews(): void {
  for (const url of previews.value.values()) URL.revokeObjectURL(url)
}

async function removeDraft(id: string): Promise<void> {
  revokePreviews()
  await remove(id)
}

onMounted(refresh)
onBeforeUnmount(revokePreviews)
</script>

<template>
  <section class="drafts-view">
    <header class="drafts-view__header">
      <p class="drafts-view__eyebrow">Publicações</p>
      <h1>Rascunhos</h1>
      <p>Publicações que você começou a criar mas ainda não publicou. Ficam salvas neste dispositivo.</p>
    </header>

    <div v-if="loading" class="drafts-view__state">Carregando rascunhos...</div>
    <div v-else-if="!drafts.length" class="drafts-view__state">
      Nenhum rascunho salvo. Ao começar uma publicação, ela é salva automaticamente aqui se você sair
      antes de publicar.
    </div>
    <ul v-else class="drafts-view__list">
      <li v-for="draft in drafts" :key="draft.id" class="drafts-view__item">
        <img
          v-if="previews.get(draft.id)"
          class="drafts-view__thumb"
          :src="previews.get(draft.id)"
          alt=""
        />
        <div class="drafts-view__info">
          <div class="drafts-view__name">
            <strong>{{ draftTitle(draft) }}</strong>
            <span class="drafts-view__badge">{{ draftTypeLabel(draft) }}</span>
          </div>
          <p class="drafts-view__meta">Salvo em: {{ formatDate(draft.updatedAt) }}</p>
        </div>
        <div class="drafts-view__actions">
          <BaseButton variant="secondary" @click="continueDraft(draft.id)">
            Continuar editando
          </BaseButton>
          <BaseButton variant="danger" @click="removeDraft(draft.id)">Excluir</BaseButton>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.drafts-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.drafts-view__header {
  margin-block-end: var(--space-8);
}
.drafts-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.drafts-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.drafts-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.drafts-view__list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  list-style: none;
}
.drafts-view__item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.drafts-view__thumb {
  width: 4rem;
  height: 4rem;
  object-fit: cover;
  border-radius: var(--radius-sm);
}
.drafts-view__info {
  flex: 1;
  min-width: 12rem;
}
.drafts-view__name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}
.drafts-view__badge {
  padding: 0.125rem var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}
.drafts-view__meta {
  margin-block-start: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.drafts-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.drafts-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
