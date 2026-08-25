<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import FeedDivider from '@/components/feed/FeedDivider.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { normalizeHttpError } from '@/api/errors'
import { useSearch } from '@/api/generated/publications/publications'
import {
  FEED_FILTERS,
  FEED_SCOPES,
  FEED_SORTS,
  isFeedFilter,
  isFeedScope,
  isFeedSort,
  useInfiniteFeed,
  type FeedFilter,
  type FeedScope,
  type FeedSort,
} from '@/features/feed/feed.queries'
import { useFeedRefresh } from '@/features/feed/useFeedRefresh'
import { useFeedViewTracking } from '@/features/feed/useFeedViewTracking'
import { useAuthStore } from '@/stores/auth.store'

const PULL_TRIGGER_DISTANCE = 70
const PULL_MAX_DISTANCE = 100
const PULL_DRAG_RATIO = 0.5
const FEED_FILTER_STORAGE_KEY = 'comes-e-bebes:feed-filter'

const FILTER_LABELS: Record<FeedFilter, string> = {
  mix: 'Tudo',
  pratos: 'Só pratos',
  receitas: 'Só receitas',
}

const SCOPE_LABELS: Record<FeedScope, string> = {
  EVERYONE: 'Todo mundo',
  FOLLOWING: 'Quem eu sigo',
  MY_COLLECTIONS: 'Minhas coleções',
}

const SORT_LABELS: Record<FeedSort, string> = {
  RECENT: 'Mais recentes',
  OLDEST: 'Mais antigas',
}

const EMPTY_STATE_TEXT: Record<FeedFilter, { title: string; description: string }> = {
  mix: {
    title: 'A cozinha está quieta por enquanto.',
    description: 'Quando novas publicações chegarem, elas aparecerão aqui.',
  },
  pratos: {
    title: 'Ainda não há pratos por aqui.',
    description: 'Quando novos pratos chegarem, eles aparecerão aqui.',
  },
  receitas: {
    title: 'Ainda não há receitas por aqui.',
    description: 'Que tal publicar a primeira?',
  },
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

interface StoredFeedFilters {
  tipo: FeedFilter
  quem: FeedScope
  ordem: FeedSort
}

function isStoredFeedFilters(value: unknown): value is StoredFeedFilters {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Partial<StoredFeedFilters>
  return isFeedFilter(candidate.tipo) && isFeedScope(candidate.quem) && isFeedSort(candidate.ordem)
}

function readStoredFilters(): StoredFeedFilters | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(FEED_FILTER_STORAGE_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    return isStoredFeedFilters(parsed) ? parsed : null
  } catch {
    return null
  }
}

function persistFilters(value: StoredFeedFilters): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(FEED_FILTER_STORAGE_KEY, JSON.stringify(value))
  } catch {
    // A preferência só dura a sessão atual quando o storage está indisponível.
  }
}

const filter = computed<FeedFilter>(() =>
  isFeedFilter(route.query.tipo) ? route.query.tipo : 'mix',
)
const scope = computed<FeedScope>(() =>
  isFeedScope(route.query.quem) ? route.query.quem : 'EVERYONE',
)
const sort = computed<FeedSort>(() => (isFeedSort(route.query.ordem) ? route.query.ordem : 'RECENT'))

const hasActiveFilters = computed(
  () => filter.value !== 'mix' || scope.value !== 'EVERYONE' || sort.value !== 'RECENT',
)

const filterSummary = computed<string | null>(() => {
  const parts: string[] = []
  if (filter.value !== 'mix') parts.push(FILTER_LABELS[filter.value].toLowerCase())
  if (scope.value === 'FOLLOWING') parts.push('de quem você segue')
  if (scope.value === 'MY_COLLECTIONS') parts.push('das suas coleções seguidas')
  if (sort.value === 'OLDEST') parts.push('da mais antiga pra mais nova')
  return parts.length > 0 ? parts.join(', ') : null
})

const panelOpen = ref(false)
const draftFilter = ref<FeedFilter>(filter.value)
const draftScope = ref<FeedScope>(scope.value)
const draftSort = ref<FeedSort>(sort.value)

function openPanel(): void {
  draftFilter.value = filter.value
  draftScope.value = scope.value
  draftSort.value = sort.value
  panelOpen.value = true
}

function togglePanel(): void {
  if (panelOpen.value) panelOpen.value = false
  else openPanel()
}

function applyFilters(next: StoredFeedFilters): void {
  void router.replace({
    query: {
      ...route.query,
      tipo: next.tipo === 'mix' ? undefined : next.tipo,
      quem: next.quem === 'EVERYONE' ? undefined : next.quem,
      ordem: next.ordem === 'RECENT' ? undefined : next.ordem,
    },
  })
  if (authStore.authenticated) persistFilters(next)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function applyDraft(): void {
  applyFilters({ tipo: draftFilter.value, quem: draftScope.value, ordem: draftSort.value })
  panelOpen.value = false
}

function clearDraft(): void {
  draftFilter.value = 'mix'
  draftScope.value = 'EVERYONE'
  draftSort.value = 'RECENT'
}

function clearFilters(): void {
  applyFilters({ tipo: 'mix', quem: 'EVERYONE', ordem: 'RECENT' })
}

onMounted(() => {
  // Visitante sempre abre sem filtro; usuário autenticado retoma a última
  // combinação escolhida quando a URL não já define nenhuma.
  const hasQueryFilters =
    isFeedFilter(route.query.tipo) || isFeedScope(route.query.quem) || isFeedSort(route.query.ordem)
  if (!hasQueryFilters && authStore.authenticated) {
    const stored = readStoredFilters()
    if (stored && (stored.tipo !== 'mix' || stored.quem !== 'EVERYONE' || stored.ordem !== 'RECENT')) {
      void router.replace({
        query: {
          ...route.query,
          tipo: stored.tipo === 'mix' ? undefined : stored.tipo,
          quem: stored.quem === 'EVERYONE' ? undefined : stored.quem,
          ordem: stored.ordem === 'RECENT' ? undefined : stored.ordem,
        },
      })
    }
  }
})

const feedQuery = useInfiniteFeed(filter, scope, sort)
const { refreshFeed } = useFeedRefresh()

// Busca por título — substitui o feed enquanto houver um termo (docs/telas/05-feed.html).
// O backend de busca só filtra por título/ingrediente, sem o filtro de tipo do feed
// nem "de quem"/"ordenar por" do mockup — não dá pra fingir que existem.
const searchTerm = ref('')
const activeSearchTerm = ref('')
const isSearching = computed(() => activeSearchTerm.value.length > 0)
const searchQuery = useSearch(
  computed(() => ({ title: activeSearchTerm.value, page: 1, size: 20 })),
  { query: { enabled: isSearching } },
)

function submitSearch(): void {
  activeSearchTerm.value = searchTerm.value.trim()
}

function clearSearch(): void {
  searchTerm.value = ''
  activeSearchTerm.value = ''
}
const loadMoreTrigger = ref<HTMLElement | null>(null)
const supportsInfiniteScroll = ref(true)
let loadMoreObserver: IntersectionObserver | null = null

const pullDistance = ref(0)
const isRefreshing = ref(false)
let pullStartY = 0
let isTrackingPull = false

function onTouchStart(event: TouchEvent): void {
  if (isRefreshing.value || window.scrollY > 0) return
  pullStartY = event.touches[0]!.clientY
  isTrackingPull = true
}

function onTouchMove(event: TouchEvent): void {
  if (!isTrackingPull) return
  if (window.scrollY > 0) {
    isTrackingPull = false
    pullDistance.value = 0
    return
  }
  const delta = event.touches[0]!.clientY - pullStartY
  if (delta <= 0) {
    pullDistance.value = 0
    return
  }
  event.preventDefault()
  pullDistance.value = Math.min(delta * PULL_DRAG_RATIO, PULL_MAX_DISTANCE)
}

async function onTouchEnd(): Promise<void> {
  if (!isTrackingPull) return
  isTrackingPull = false
  const shouldRefresh = pullDistance.value >= PULL_TRIGGER_DISTANCE
  pullDistance.value = 0
  if (!shouldRefresh) return
  isRefreshing.value = true
  await refreshFeed()
  isRefreshing.value = false
}

const publications = computed(
  () => feedQuery.data.value?.pages.flatMap((page) => page.content) ?? [],
)
const firstViewedIndex = computed(() =>
  publications.value.findIndex((publication) => publication.viewedByCurrentUser),
)
const { cardRef } = useFeedViewTracking()
const errorMessage = computed(() =>
  feedQuery.error.value
    ? normalizeHttpError(feedQuery.error.value).message
    : 'Não foi possível carregar o feed.',
)

watch(loadMoreTrigger, (current, previous) => {
  if (previous) loadMoreObserver?.unobserve(previous)
  if (current) loadMoreObserver?.observe(current)
})

onMounted(() => {
  if (typeof IntersectionObserver === 'undefined') {
    supportsInfiniteScroll.value = false
    return
  }

  try {
    loadMoreObserver = new IntersectionObserver(
      ([entry]) => {
        if (
          entry?.isIntersecting &&
          feedQuery.hasNextPage.value &&
          !feedQuery.isFetchingNextPage.value
        ) {
          void feedQuery.fetchNextPage()
        }
      },
      // IntersectionObserver só aceita rootMargin em px ou %; um valor em rem
      // faz o construtor lançar exceção em navegadores reais.
      { rootMargin: '320px' },
    )
  } catch {
    supportsInfiniteScroll.value = false
    loadMoreObserver = null
  }

  if (loadMoreObserver && loadMoreTrigger.value) {
    loadMoreObserver.observe(loadMoreTrigger.value)
  }
})

onBeforeUnmount(() => loadMoreObserver?.disconnect())
</script>

<template>
  <section
    class="feed-view"
    aria-labelledby="feed-title"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
    @touchcancel="onTouchEnd"
  >
    <div
      v-if="pullDistance > 0 || isRefreshing"
      class="feed-view__pull-indicator"
      role="status"
      :style="{ height: `${isRefreshing ? PULL_TRIGGER_DISTANCE : pullDistance}px` }"
    >
      {{
        isRefreshing
          ? 'Atualizando…'
          : pullDistance >= PULL_TRIGGER_DISTANCE
            ? 'Solte para atualizar'
            : 'Puxe para atualizar'
      }}
    </div>
    <header class="feed-view__heading">
      <p>Da cozinha da comunidade</p>
      <h1 id="feed-title">O que andaram cozinhando</h1>
      <span>Publicações recentes de quem você segue e da comunidade.</span>
    </header>

    <form class="feed-view__search" role="search" @submit.prevent="submitSearch">
      <AppIcon name="search" :size="20" :stroke-width="1.8" class="feed-view__search-icon" />
      <input
        v-model="searchTerm"
        type="search"
        placeholder="Buscar por título..."
        aria-label="Buscar publicações por título"
      />
      <button v-if="searchTerm" type="button" aria-label="Limpar busca" @click="clearSearch">
        <AppIcon name="close" :size="18" :stroke-width="2" />
      </button>
    </form>

    <template v-if="isSearching">
      <p class="feed-view__summary">
        Mostrando resultados para <strong>"{{ activeSearchTerm }}"</strong>
        <button type="button" @click="clearSearch">Limpar</button>
      </p>

      <div v-if="searchQuery.isPending.value" class="feed-view__skeletons" aria-label="Buscando">
        <div v-for="index in 2" :key="index" class="feed-view__skeleton" />
      </div>
      <div v-else-if="!searchQuery.data.value?.content.length" class="feed-view__state">
        <AppIcon name="search" :size="40" :stroke-width="1.4" />
        <strong>Nada encontrado</strong>
        <p>Tente simplificar a busca ou procurar por outro termo.</p>
        <BaseButton @click="clearSearch">Limpar a busca</BaseButton>
      </div>
      <div v-else class="feed-view__list">
        <PublicationCard
          v-for="publication in searchQuery.data.value.content"
          :key="publication.id"
          :publication="publication"
        />
      </div>
    </template>

    <template v-else>
      <div class="feed-view__toolbar">
        <button
          type="button"
          class="feed-view__filter-btn"
          :class="{ 'feed-view__filter-btn--active': hasActiveFilters }"
          :aria-expanded="panelOpen"
          @click="togglePanel"
        >
          <AppIcon name="filter" :size="19" :stroke-width="2" />
          Filtros
          <span v-if="hasActiveFilters" class="feed-view__filter-dot" aria-hidden="true" />
        </button>
      </div>

      <div v-if="panelOpen" class="feed-view__panel">
        <div class="feed-view__panel-row">
          <b>Mostrar</b>
          <div class="feed-view__chips" role="group" aria-label="Mostrar">
            <button
              v-for="option in FEED_FILTERS"
              :key="option"
              type="button"
              class="feed-view__chip"
              :aria-pressed="draftFilter === option"
              @click="draftFilter = option"
            >
              {{ FILTER_LABELS[option] }}
            </button>
          </div>
        </div>

        <div v-if="authStore.authenticated" class="feed-view__panel-row">
          <b>De quem</b>
          <div class="feed-view__chips" role="group" aria-label="De quem">
            <button
              v-for="option in FEED_SCOPES"
              :key="option"
              type="button"
              class="feed-view__chip"
              :aria-pressed="draftScope === option"
              @click="draftScope = option"
            >
              {{ SCOPE_LABELS[option] }}
            </button>
          </div>
        </div>

        <div class="feed-view__panel-row">
          <b>Ordenar por</b>
          <div class="feed-view__chips" role="group" aria-label="Ordenar por">
            <button
              v-for="option in FEED_SORTS"
              :key="option"
              type="button"
              class="feed-view__chip"
              :aria-pressed="draftSort === option"
              @click="draftSort = option"
            >
              {{ SORT_LABELS[option] }}
            </button>
          </div>
        </div>

        <div class="feed-view__panel-foot">
          <BaseButton variant="secondary" @click="clearDraft">Limpar tudo</BaseButton>
          <BaseButton @click="applyDraft">Ver resultados</BaseButton>
        </div>
      </div>

      <p v-if="filterSummary" class="feed-view__summary">
        Mostrando <strong>{{ filterSummary }}</strong>
        <button type="button" @click="clearFilters">Limpar</button>
      </p>

      <div v-if="feedQuery.isPending.value" class="feed-view__skeletons" aria-label="Carregando feed">
        <div v-for="index in 2" :key="index" class="feed-view__skeleton" />
      </div>

      <div v-else-if="feedQuery.isError.value" class="feed-view__state" role="alert">
        <strong>Não conseguimos abrir o feed.</strong>
        <p>{{ errorMessage }}</p>
        <BaseButton @click="feedQuery.refetch()">Tentar novamente</BaseButton>
      </div>

      <div v-else-if="publications.length === 0" class="feed-view__state">
        <AppIcon name="home" :size="40" :stroke-width="1.4" />
        <strong>{{ EMPTY_STATE_TEXT[filter].title }}</strong>
        <p>{{ EMPTY_STATE_TEXT[filter].description }}</p>
        <BaseButton v-if="authStore.authenticated" @click="router.push('/publicar')">
          Publicar algo que você fez
        </BaseButton>
      </div>

      <template v-else>
        <div class="feed-view__list">
          <template v-for="(publication, index) in publications" :key="publication.id">
            <FeedDivider v-if="index === firstViewedIndex" />
            <div :ref="cardRef(publication)">
              <PublicationCard :publication="publication" />
            </div>
          </template>
        </div>

        <div ref="loadMoreTrigger" class="feed-view__pagination">
          <p v-if="feedQuery.isFetchingNextPage.value" role="status">
            Carregando mais publicações…
          </p>
          <BaseButton
            v-else-if="feedQuery.hasNextPage.value && !supportsInfiniteScroll"
            variant="secondary"
            :loading="feedQuery.isFetchingNextPage.value"
            @click="feedQuery.fetchNextPage()"
          >
            Carregar mais publicações
          </BaseButton>
          <span v-else-if="feedQuery.hasNextPage.value" aria-hidden="true" />
          <p v-else>Você chegou ao fim por hoje.</p>
        </div>
      </template>
    </template>
  </section>
</template>

<style scoped>
.feed-view {
  width: min(100%, var(--content-feed));
  margin-inline: auto;
}

.feed-view__pull-indicator {
  display: flex;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
  transition: height var(--duration-fast) var(--ease-standard);
}

.feed-view__heading {
  display: grid;
  gap: var(--space-2);
  margin-block-end: var(--space-8);
}

.feed-view__heading p {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.feed-view__heading h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
}

.feed-view__heading span {
  color: var(--color-text-secondary);
}

.feed-view__search {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-height: var(--control-min-size);
  padding-inline: var(--space-4);
  margin-block-end: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.feed-view__search-icon {
  flex: none;
  color: var(--color-text-secondary);
}

.feed-view__search input {
  width: 100%;
  min-height: var(--control-min-size);
  color: var(--color-text);
  background: transparent;
  border: 0;
}

.feed-view__search input:focus-visible {
  outline: none;
}

.feed-view__search button {
  display: grid;
  flex: none;
  width: 1.75rem;
  height: 1.75rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--radius-pill);
  place-items: center;
}

.feed-view__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  margin-block-end: var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-surface));
  border-radius: var(--radius-md);
}

.feed-view__summary strong {
  color: var(--color-text);
}

.feed-view__summary button {
  margin-inline-start: auto;
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
}

.feed-view__toolbar {
  display: flex;
  margin-block-end: var(--space-4);
}

.feed-view__filter-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: var(--control-min-size);
  padding-inline: var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.feed-view__filter-btn:hover {
  background: var(--color-surface-raised);
}

.feed-view__filter-btn--active {
  border-color: var(--color-primary);
}

.feed-view__filter-dot {
  width: 0.5625rem;
  height: 0.5625rem;
  background: var(--color-primary);
  border-radius: var(--radius-pill);
}

.feed-view__panel {
  padding: var(--space-5);
  margin-block-end: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.feed-view__panel-row {
  margin-block-end: var(--space-5);
}

.feed-view__panel-row > b {
  display: block;
  margin-block-end: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.feed-view__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.feed-view__chip {
  min-height: 2.75rem;
  padding-inline: var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.feed-view__chip:hover {
  border-color: var(--color-text-secondary);
}

.feed-view__chip[aria-pressed='true'] {
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.feed-view__panel-foot {
  display: flex;
  gap: var(--space-3);
  padding-block-start: var(--space-4);
  border-block-start: 1px solid var(--color-border);
}

.feed-view__panel-foot > * {
  flex: 1;
}

.feed-view__list,
.feed-view__skeletons {
  display: grid;
  gap: var(--space-8);
}

.feed-view__skeleton {
  min-height: 42rem;
  background: linear-gradient(
    100deg,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 30%,
    color-mix(in srgb, var(--color-border) 58%, var(--color-surface)) 50%,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 70%
  );
  background-size: 200% 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  animation: feed-skeleton var(--duration-slow) linear infinite;
}

.feed-view__state {
  display: grid;
  min-height: 22rem;
  place-content: center;
  justify-items: center;
  gap: var(--space-3);
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.feed-view__state > svg {
  color: var(--color-primary);
}

.feed-view__state strong {
  color: var(--color-text);
  font-family: var(--font-editorial);
  font-size: var(--font-size-xl);
}

.feed-view__state p {
  max-width: 28rem;
  margin: 0;
}

.feed-view__pagination {
  display: grid;
  place-items: center;
  min-height: 10rem;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.feed-view__pagination p {
  margin: 0;
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

@keyframes feed-skeleton {
  to {
    background-position: -200% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .feed-view__skeleton {
    animation: none;
  }
}
</style>
