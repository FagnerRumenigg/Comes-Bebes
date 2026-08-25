import { useInfiniteQuery } from '@tanstack/vue-query'

import { saved } from '@/api/generated/publications/publications'

export const SAVED_PAGE_SIZE = 20

export function useInfiniteSaved() {
  return useInfiniteQuery({
    queryKey: ['publications', 'saved'] as const,
    initialPageParam: 1,
    queryFn: ({ pageParam, signal }) => saved({ page: pageParam, size: SAVED_PAGE_SIZE }, undefined, signal),
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })
}
