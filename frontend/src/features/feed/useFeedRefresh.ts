import { useQueryClient } from '@tanstack/vue-query'

import { feedQueryKeys } from './feed.queries'

export function useFeedRefresh() {
  const queryClient = useQueryClient()

  function refreshFeed(): Promise<void> {
    return queryClient.resetQueries({ queryKey: feedQueryKeys.all })
  }

  return { refreshFeed }
}
