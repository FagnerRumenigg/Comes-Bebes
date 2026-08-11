<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import BaseButton from '@/components/base/BaseButton.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { normalizeHttpError } from '@/api/errors'
import { useInfiniteFeed } from '@/features/feed/feed.queries'

const feedQuery = useInfiniteFeed()
const loadMoreTrigger = ref<HTMLElement | null>(null)
const supportsInfiniteScroll = ref(true)
let loadMoreObserver: IntersectionObserver | null = null

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
  <section class="feed-view" aria-labelledby="feed-title">
    <header class="feed-view__heading">
      <p>Da cozinha da comunidade</p>
      <h1 id="feed-title">Seu feed</h1>
      <span>Publicações recentes, em ordem cronológica.</span>
    </header>

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
      <strong>A cozinha está quieta por enquanto.</strong>
      <p>Quando novas publicações chegarem, elas aparecerão aqui.</p>
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
