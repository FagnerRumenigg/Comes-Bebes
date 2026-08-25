import { useInfiniteQuery } from '@tanstack/vue-query'
import { computed, unref, type MaybeRef } from 'vue'

import { feed } from '@/api/generated/publications/publications'
import { FeedTypesItem } from '@/api/generated/models'
import type { FeedParams } from '@/api/generated/models'
import { useAuthStore } from '@/stores/auth.store'

export const FEED_PAGE_SIZE = 10

export type FeedFilter = 'mix' | 'pratos' | 'receitas'

export const FEED_FILTERS: FeedFilter[] = ['mix', 'pratos', 'receitas']

const FEED_FILTER_TYPES: Record<FeedFilter, FeedTypesItem[] | undefined> = {
  mix: undefined,
  pratos: [FeedTypesItem.DISH],
  receitas: [FeedTypesItem.RECIPE, FeedTypesItem.MY_VERSION],
}

export function isFeedFilter(value: unknown): value is FeedFilter {
  return typeof value === 'string' && (FEED_FILTERS as string[]).includes(value)
}

// Facets "De quem" e "Ordenar por" (docs/telas/05-feed.html). O backend já
// aceita os dois em GET /publications/feed, mas o orval ainda não sabe disso
// (precisa do backend rodando uma vez pra reexportar openapi.json) — por
// isso o tipo é estendido na mão em vez de vir de src/api/generated.
export type FeedScope = 'EVERYONE' | 'FOLLOWING' | 'MY_COLLECTIONS'
export type FeedSort = 'RECENT' | 'OLDEST'

export const FEED_SCOPES: FeedScope[] = ['EVERYONE', 'FOLLOWING', 'MY_COLLECTIONS']
export const FEED_SORTS: FeedSort[] = ['RECENT', 'OLDEST']

export function isFeedScope(value: unknown): value is FeedScope {
  return typeof value === 'string' && (FEED_SCOPES as string[]).includes(value)
}

export function isFeedSort(value: unknown): value is FeedSort {
  return typeof value === 'string' && (FEED_SORTS as string[]).includes(value)
}

type ExtendedFeedParams = FeedParams & { scope?: FeedScope; sort?: FeedSort }

export const feedQueryKeys = {
  all: ['publications', 'feed'] as const,
  list: (
    audience: MaybeRef<'public' | 'authenticated'>,
    filter: MaybeRef<FeedFilter>,
    scope: MaybeRef<FeedScope>,
    sort: MaybeRef<FeedSort>,
    size = FEED_PAGE_SIZE,
  ) => [...feedQueryKeys.all, { audience, filter, scope, sort, size }] as const,
}

export function useInfiniteFeed(
  filter: MaybeRef<FeedFilter>,
  scope: MaybeRef<FeedScope>,
  sort: MaybeRef<FeedSort>,
) {
  const authStore = useAuthStore()

  const audience = computed(() => (authStore.authenticated ? 'authenticated' : 'public'))

  return useInfiniteQuery({
    queryKey: feedQueryKeys.list(audience, filter, scope, sort),
    initialPageParam: 1,
    queryFn: ({ pageParam, signal }) => {
      const params: ExtendedFeedParams = {
        page: pageParam,
        size: FEED_PAGE_SIZE,
        types: FEED_FILTER_TYPES[unref(filter)],
        scope: unref(scope) === 'EVERYONE' ? undefined : unref(scope),
        sort: unref(sort) === 'RECENT' ? undefined : unref(sort),
      }
      return feed(params, undefined, signal)
    },
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })
}
