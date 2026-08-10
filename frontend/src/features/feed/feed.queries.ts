import { useInfiniteQuery } from '@tanstack/vue-query'
import { computed, type MaybeRef } from 'vue'

import { feed } from '@/api/generated/publications/publications'
import { useAuthStore } from '@/stores/auth.store'

export const FEED_PAGE_SIZE = 4

export const feedQueryKeys = {
  all: ['publications', 'feed'] as const,
  list: (audience: MaybeRef<'public' | 'authenticated'>, size = FEED_PAGE_SIZE) =>
    [...feedQueryKeys.all, { audience, size }] as const,
}

export function useInfiniteFeed() {
  const authStore = useAuthStore()

  const audience = computed(() => (authStore.authenticated ? 'authenticated' : 'public'))

  return useInfiniteQuery({
    queryKey: feedQueryKeys.list(audience),
    initialPageParam: 1,
    queryFn: ({ pageParam, signal }) =>
      feed({ page: pageParam, size: FEED_PAGE_SIZE }, undefined, signal),
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })
}
