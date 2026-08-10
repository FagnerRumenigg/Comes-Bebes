import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import type { App } from 'vue'

import { shouldRetryQuery } from '@/api/errors'
import { router } from '@/app/router'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: shouldRetryQuery,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})

export const pinia = createPinia()

export function installApplicationPlugins(app: App): void {
  app.use(pinia)
  app.use(router)
  app.use(VueQueryPlugin, { queryClient })
}
