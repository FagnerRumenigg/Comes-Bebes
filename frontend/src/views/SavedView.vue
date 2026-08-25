<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueries, useQueryClient } from '@tanstack/vue-query'

import { getGetUserCollectionsQueryKey, useGetUserCollections } from '@/api/generated/users/users'
import { useGetFollowedCollections } from '@/api/generated/collections/collections'
import { getGetPublicationByIdQueryOptions } from '@/api/generated/publications/publications'
import BaseButton from '@/components/base/BaseButton.vue'
import CollectionCard from '@/components/collection/CollectionCard.vue'
import CollectionFormDialog from '@/components/collection/CollectionFormDialog.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import PublicationTile from '@/components/publication/PublicationTile.vue'
import { useInfiniteSaved } from '@/features/saved/saved.queries'
import { useAuthStore } from '@/stores/auth.store'

// Quantas publicações mostrar antes de "Ver todas" quando existem coleções
// pra organizar visualmente a página (docs/telas/06-salvos.html).
const PREVIEW_COUNT = 6

const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const userId = computed(() => authStore.identity?.userId ?? '')

const ownCollectionsQuery = useGetUserCollections(userId, { page: 1, size: 20 })
const followedCollectionsQuery = useGetFollowedCollections({ page: 1, size: 20 })

const hasCollections = computed(
  () =>
    Boolean(ownCollectionsQuery.data.value?.content.length) ||
    Boolean(followedCollectionsQuery.data.value?.content.length),
)

const savedInfinite = useInfiniteSaved()
const expanded = ref(false)

const savedItemsAll = computed(
  () => savedInfinite.data.value?.pages.flatMap((page) => page.content) ?? [],
)
const savedTotal = computed(() => savedInfinite.data.value?.pages[0]?.totalElements ?? 0)
const showSeeAll = computed(
  () => hasCollections.value && !expanded.value && savedTotal.value > PREVIEW_COUNT,
)
const visibleSavedItems = computed(() =>
  hasCollections.value && !expanded.value
    ? savedItemsAll.value.slice(0, PREVIEW_COUNT)
    : savedItemsAll.value,
)

function showAllSaved(): void {
  expanded.value = true
}

const details = useQueries({
  queries: computed(() =>
    visibleSavedItems.value.map((item) => getGetPublicationByIdQueryOptions(item.publicationId)),
  ),
})
const savedPublications = computed(() =>
  details.value
    .map((query) => query.data)
    .filter((publication): publication is NonNullable<typeof publication> => Boolean(publication))
    .map((publication) => ({ ...publication, saved: true })),
)
const detailsPending = computed(() => details.value.some((query) => query.isPending))

const newCollectionOpen = ref(false)

function handleCollectionCreated(): void {
  void queryClient.invalidateQueries({ queryKey: getGetUserCollectionsQueryKey(userId) })
}

const hasAnything = computed(() => hasCollections.value || savedTotal.value > 0)

const showNudge = computed(
  () =>
    !ownCollectionsQuery.isPending.value &&
    !ownCollectionsQuery.data.value?.content.length &&
    savedTotal.value > 0,
)
</script>

<template>
  <section class="saved-view">
    <header class="saved-view__header">
      <div>
        <h1>Salvos</h1>
        <p>Aqui ficam as receitas que você guardar para fazer depois.</p>
      </div>
      <BaseButton variant="secondary" @click="newCollectionOpen = true">
        <AppIcon name="plus" :size="18" :stroke-width="2.2" />
        Nova coleção
      </BaseButton>
    </header>

    <CollectionFormDialog v-model:open="newCollectionOpen" @saved="handleCollectionCreated" />

    <div
      v-if="savedInfinite.isPending.value || ownCollectionsQuery.isPending.value"
      class="saved-view__state"
    >
      Carregando salvos...
    </div>

    <div v-else-if="!hasAnything" class="saved-view__empty">
      <AppIcon name="bookmark" :size="40" :stroke-width="1.4" />
      <p><strong>Nada guardado ainda</strong></p>
      <p>
        Achou uma receita que quer fazer no fim de semana? Toque no marcador e ela fica aqui,
        esperando.
      </p>
      <span class="saved-view__empty-chip">
        <AppIcon name="bookmark" :size="15" :stroke-width="2" />
        Procure este símbolo nas publicações
      </span>
      <BaseButton @click="router.push('/')">Ver o que andaram cozinhando</BaseButton>
    </div>

    <template v-else>
      <p v-if="showNudge" class="saved-view__nudge">
        <strong>Quer separar por tipo?</strong>
        Crie uma coleção e junte o que combina — sobremesas, almoço rápido, o que você quiser.
        <BaseButton variant="secondary" @click="newCollectionOpen = true">
          Criar coleção
        </BaseButton>
      </p>

      <section v-if="ownCollectionsQuery.data.value?.content.length" class="saved-view__section">
        <h2>Suas coleções ({{ ownCollectionsQuery.data.value.content.length }})</h2>
        <div class="saved-view__grid">
          <CollectionCard
            v-for="collection in ownCollectionsQuery.data.value.content"
            :key="collection.id"
            :collection="collection"
          />
          <button type="button" class="saved-view__new-tile" @click="newCollectionOpen = true">
            <AppIcon name="plus" :size="24" :stroke-width="1.8" />
            Nova coleção
          </button>
        </div>
      </section>

      <section
        v-if="followedCollectionsQuery.data.value?.content.length"
        class="saved-view__section"
      >
        <h2>Coleções que você segue ({{ followedCollectionsQuery.data.value.content.length }})</h2>
        <div class="saved-view__grid">
          <CollectionCard
            v-for="collection in followedCollectionsQuery.data.value.content"
            :key="collection.id"
            :collection="collection"
            show-author
          />
        </div>
      </section>

      <section v-if="savedTotal > 0" class="saved-view__section">
        <div class="saved-view__section-head">
          <h2>Tudo que você guardou ({{ savedTotal }})</h2>
          <button v-if="showSeeAll" type="button" class="saved-view__more" @click="showAllSaved">
            Ver todas
          </button>
        </div>
        <div v-if="detailsPending" class="saved-view__state">Carregando publicações...</div>
        <div v-else class="saved-view__grid">
          <PublicationTile
            v-for="publication in savedPublications"
            :key="publication.id"
            :publication="publication"
            show-author
          />
        </div>
        <BaseButton
          v-if="expanded && savedInfinite.hasNextPage.value"
          variant="secondary"
          class="saved-view__load-more"
          :loading="savedInfinite.isFetchingNextPage.value"
          @click="savedInfinite.fetchNextPage()"
        >
          Carregar mais
        </BaseButton>
      </section>
    </template>
  </section>
</template>

<style scoped>
.saved-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}

.saved-view__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-block-end: var(--space-8);
}

.saved-view__header p {
  margin-block-start: var(--space-2);
  color: var(--color-text-secondary);
}

.saved-view__section {
  margin-block-end: var(--space-10);
}

.saved-view__section h2 {
  margin: 0 0 var(--space-4);
  font-size: var(--font-size-xl);
}

.saved-view__section-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.saved-view__section-head h2 {
  flex: 1;
}

.saved-view__more {
  margin-block-end: var(--space-4);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
}

.saved-view__more:hover {
  text-decoration: underline;
}

.saved-view__load-more {
  margin-block-start: var(--space-5);
}

.saved-view__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(9rem, 1fr));
  gap: var(--space-4) var(--space-3);
}

.saved-view__new-tile {
  display: grid;
  aspect-ratio: 1;
  align-content: center;
  justify-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  background: transparent;
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.saved-view__new-tile:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.saved-view__nudge {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  margin-block-end: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.saved-view__nudge strong {
  flex-basis: 100%;
  color: var(--color-text);
}

.saved-view__empty {
  display: grid;
  gap: var(--space-3);
  justify-items: center;
  padding: var(--space-12) var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.saved-view__empty svg {
  color: var(--color-primary);
}

.saved-view__empty p {
  max-width: 26rem;
  margin: 0;
}

.saved-view__empty strong {
  color: var(--color-text);
}

.saved-view__empty-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.saved-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
