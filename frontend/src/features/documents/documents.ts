import { useQuery } from '@tanstack/vue-query'
import { computed, type ComputedRef } from 'vue'

import { apiRequest } from '@/api/client'

/**
 * GET /documents/{slug} é endpoint novo, ainda sem passar pelo orval —
 * chama direto até `npm run api:generate` rodar de novo.
 */
export type DocumentSlug = 'TERMS_OF_SERVICE' | 'PRIVACY_POLICY' | 'FAQ'

export interface DocumentContent {
  slug: DocumentSlug
  title: string
  body: string
  updatedAt: string
}

export function useDocument(slug: ComputedRef<DocumentSlug> | DocumentSlug) {
  const slugRef = computed(() => (typeof slug === 'string' ? slug : slug.value))

  return useQuery({
    queryKey: computed(() => ['documents', slugRef.value] as const),
    queryFn: ({ signal }) =>
      apiRequest<DocumentContent>({ url: `/documents/${slugRef.value}`, method: 'GET', signal }),
  })
}
