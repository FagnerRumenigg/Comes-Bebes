import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'

import { apiRequest } from '@/api/client'
import { useAuthStore } from '@/stores/auth.store'

/**
 * GET /auth/info ainda não passou pelo orval — o client gerado só é
 * atualizado rodando o backend uma vez pra exportar openapi.json e depois
 * `npm run api:generate`. Até isso acontecer, chama o endpoint direto.
 * Depois, trocar por um hook gerado em src/api/generated/authentication e
 * apagar este arquivo.
 */
interface AuthInfoResponse {
  displayName: string
  defaultPublicationVisibility: 'PUBLIC' | 'INTERNAL' | 'PRIVATE'
}

export function getAccountInfoQueryKey(userId: string | null) {
  return ['auth-info', userId] as const
}

export function useAccountInfo() {
  const authStore = useAuthStore()
  const userId = computed(() => authStore.identity?.userId ?? null)

  const query = useQuery({
    queryKey: computed(() => getAccountInfoQueryKey(userId.value)),
    queryFn: () => apiRequest<AuthInfoResponse>({ url: '/auth/info', method: 'GET' }),
    enabled: computed(() => authStore.authenticated),
    staleTime: 5 * 60 * 1000,
  })

  return {
    displayName: computed(() => query.data.value?.displayName),
    defaultPublicationVisibility: computed(() => query.data.value?.defaultPublicationVisibility),
  }
}
