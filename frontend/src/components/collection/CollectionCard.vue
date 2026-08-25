<script setup lang="ts">
import { computed } from 'vue'

import type { CollectionResponse } from '@/api/generated/models'
import AppIcon from '@/components/icons/AppIcon.vue'
import { resolveImageUrl } from '@/utils/resolveImageUrl'

// coverImageUrls ainda não passou pelo orval (precisa do backend rodando pra
// reexportar openapi.json) — o campo já existe na resposta real do backend
// (docs/telas/06-salvos.html, mosaico do cartão), só o tipo gerado que não
// sabe disso ainda. Trocar por CollectionResponse puro depois de regenerar.
type CollectionWithCover = CollectionResponse & { coverImageUrls?: string[] }

const props = defineProps<{
  collection: CollectionWithCover
  /** Mostra "de {autor}" no lugar da contagem — usado em grids de coleções de terceiros. */
  showAuthor?: boolean
}>()

// Sempre o último prato adicionado à coleção — não é mais um mosaico de várias fotos.
const coverImageUrl = computed(() => props.collection.coverImageUrls?.[0] ?? null)

// Vocabulário exato de produto5.md v5 §6.3 — "Pública" não leva emblema (é o padrão).
const visibility = computed(() => {
  if (props.collection.visibility === 'SHARED') {
    return { label: 'Para quem eu escolher', icon: 'people' as const }
  }
  if (props.collection.visibility === 'PRIVATE') {
    return { label: 'Só para mim', icon: 'lock' as const }
  }
  return null
})
</script>

<template>
  <RouterLink class="collection-card" :to="`/colecoes/${collection.id}`">
    <span
      class="collection-card__cover"
      aria-hidden="true"
      :style="coverImageUrl ? { backgroundImage: `url(${resolveImageUrl(coverImageUrl)})` } : undefined"
    >
      <AppIcon v-if="!coverImageUrl" name="bookmark" :size="28" :stroke-width="1.6" />
    </span>
    <h3 class="collection-card__name">{{ collection.name }}</h3>
    <span class="collection-card__meta">
      <template v-if="showAuthor">de {{ collection.authorDisplayName }} · </template>
      {{ collection.publicationsCount }}
      {{ collection.publicationsCount === 1 ? 'receita' : 'receitas' }}
      <template v-if="!showAuthor && visibility">
        · <AppIcon :name="visibility.icon" :size="12" :stroke-width="2.2" class="collection-card__badge-icon" />
        {{ visibility.label }}
      </template>
    </span>
  </RouterLink>
</template>

<style scoped>
.collection-card {
  display: block;
  color: inherit;
  text-decoration: none;
}

.collection-card__cover {
  display: grid;
  aspect-ratio: 1;
  overflow: hidden;
  color: var(--color-primary);
  background-color: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
  background-position: center;
  background-size: cover;
  border-radius: var(--radius-md);
  place-items: center;
}

.collection-card__name {
  display: block;
  overflow: hidden;
  margin: var(--space-2) 0 0;
  color: var(--color-text);
  font-family: var(--font-interface);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  letter-spacing: normal;
  text-overflow: ellipsis;
  text-wrap: initial;
  white-space: nowrap;
}

.collection-card__meta {
  display: flex;
  align-items: center;
  gap: 0.1875rem;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collection-card__badge-icon {
  flex: none;
}
</style>
