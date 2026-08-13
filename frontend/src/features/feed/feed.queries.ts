import { useInfiniteQuery } from '@tanstack/vue-query'
import { computed, unref, type MaybeRef } from 'vue'

import { feed } from '@/api/generated/publications/publications'
import { FeedTypesItem } from '@/api/generated/models'
import { useAuthStore } from '@/stores/auth.store'

export const FEED_PAGE_SIZE = 50

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

export const feedQueryKeys = {
  all: ['publications', 'feed'] as const,
  list: (
    audience: MaybeRef<'public' | 'authenticated'>,
    filter: MaybeRef<FeedFilter>,
    size = FEED_PAGE_SIZE,
  ) => [...feedQueryKeys.all, { audience, filter, size }] as const,
}

export function useInfiniteFeed(filter: MaybeRef<FeedFilter>) {
  const authStore = useAuthStore()

  const audience = computed(() => (authStore.authenticated ? 'authenticated' : 'public'))

  return useInfiniteQuery({
    queryKey: feedQueryKeys.list(audience, filter),
    initialPageParam: 1,
    queryFn: ({ pageParam, signal }) =>
      feed(
        { page: pageParam, size: FEED_PAGE_SIZE, types: FEED_FILTER_TYPES[unref(filter)] },
        undefined,
        signal,
      ),
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })
}
