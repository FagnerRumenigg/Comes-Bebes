<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { normalizeHttpError } from '@/api/errors'
import { FEED_FILTERS, isFeedFilter, useInfiniteFeed, type FeedFilter } from '@/features/feed/feed.queries'
import { useFeedRefresh } from '@/features/feed/useFeedRefresh'
import { useAuthStore } from '@/stores/auth.store'

const PULL_TRIGGER_DISTANCE = 70
const PULL_MAX_DISTANCE = 100
const PULL_DRAG_RATIO = 0.5
const FEED_FILTER_STORAGE_KEY = 'comes-e-bebes:feed-filter'

const FILTER_LABELS: Record<FeedFilter, string> = {
  mix: 'Mix',
  pratos: 'Pratos',
  receitas: 'Receitas',
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

function readStoredFilter(): FeedFilter | null {
  if (typeof window === 'undefined') return null
  try {
    const value = window.localStorage.getItem(FEED_FILTER_STORAGE_KEY)
    return isFeedFilter(value) ? value : null
  } catch {
    return null
  }
}

function persistFilter(value: FeedFilter): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(FEED_FILTER_STORAGE_KEY, value)
  } catch {
    // A preferência só dura a sessão atual quando o storage está indisponível.
  }
}

const filter = computed<FeedFilter>(() =>
  isFeedFilter(route.query.tipo) ? route.query.tipo : 'mix',
)

function selectFilter(next: FeedFilter): void {
  if (filter.value === next) return
  void router.replace({ query: { ...route.query, tipo: next === 'mix' ? undefined : next } })
  if (authStore.authenticated) persistFilter(next)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  // Visitante sempre abre em Mix; usuário autenticado retoma a última
  // visualização escolhida quando a URL não já define um filtro.
  if (!isFeedFilter(route.query.tipo) && authStore.authenticated) {
    const stored = readStoredFilter()
    if (stored && stored !== 'mix') {
      void router.replace({ query: { ...route.query, tipo: stored } })
    }
  }
})

const feedQuery = useInfiniteFeed(filter)
const { refreshFeed } = useFeedRefresh()
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
      <h1 id="feed-title">Seu feed</h1>
      <span>Publicações recentes, em ordem cronológica.</span>
    </header>

    <div class="feed-view__filters" role="group" aria-label="Filtrar por tipo de publicação">
      <button
        v-for="option in FEED_FILTERS"
        :key="option"
        type="button"
        class="feed-view__filter"
        :class="{ 'feed-view__filter--active': filter === option }"
        :aria-pressed="filter === option"
        @click="selectFilter(option)"
      >
        {{ FILTER_LABELS[option] }}
      </button>
    </div>

    <div v-if="feedQuery.isPending.value" class="feed-view__skeletons" aria-label="Carregando feed">
      <div v-for="index in 2" :key="index" class="feed-view__skeleton" />
    </div>

    <div v-else-if="feedQuery.isError.value" class="feed-view__state" role="alert">
      <strong>Não conseguimos abrir o feed.</strong>
      <p>{{ errorMessage }}</p>
      <BaseButton @click="feedQuery.refetch()">Tentar novamente</BaseButton>
    </div>

    <div v-else-if="publications.length === 0" class="feed-view__state">
      <span aria-hidden="true">◇</span>
      <strong>{{ EMPTY_STATE_TEXT[filter].title }}</strong>
      <p>{{ EMPTY_STATE_TEXT[filter].description }}</p>
    </div>

    <template v-else>
      <div class="feed-view__list">
        <PublicationCard
          v-for="publication in publications"
          :key="publication.id"
          :publication="publication"
        />
      </div>

      <div ref="loadMoreTrigger" class="feed-view__pagination">
        <p v-if="feedQuery.isFetchingNextPage.value" role="status">Carregando mais publicações…</p>
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

.feed-view__filters {
  display: flex;
  gap: var(--space-2);
  margin-block-end: var(--space-8);
  overflow-x: auto;
}

.feed-view__filter {
  min-height: 2.25rem;
  padding: var(--space-2) var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.feed-view__filter:hover:not(.feed-view__filter--active) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.feed-view__filter--active {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-color: var(--color-primary);
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

.feed-view__state > span {
  color: var(--color-primary);
  font-size: var(--font-size-3xl);
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
