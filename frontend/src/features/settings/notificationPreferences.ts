import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, type ComputedRef } from 'vue'

import { apiRequest } from '@/api/client'

/**
 * GET/PATCH /users/{id}/notification-preferences ganharam 6 campos novos
 * (docs/telas/09-configuracoes.html, seção "Avisos") que o client gerado
 * ainda não conhece — orval só atualiza rodando o backend uma vez. Até lá,
 * chama os endpoints direto. Trocar pelos hooks gerados depois de rodar
 * `npm run api:generate`.
 */
export interface NotificationPreferences {
  notifyOnFollowedPublish: boolean
  notifyOnSaved: boolean
  notifyOnReacted: boolean
  notifyOnMyVersion: boolean
  notifyOnCollectionNewItem: boolean
  notifyOnCollectionShared: boolean
  notifyWeeklyEmail: boolean
}

export type UpdateNotificationPreferences = Partial<NotificationPreferences>

function queryKey(userId: string) {
  return ['users', userId, 'notification-preferences'] as const
}

export function useNotificationPreferencesQuery(userId: ComputedRef<string>, enabled: ComputedRef<boolean>) {
  return useQuery({
    queryKey: computed(() => queryKey(userId.value)),
    queryFn: ({ signal }) =>
      apiRequest<NotificationPreferences>({
        url: `/users/${userId.value}/notification-preferences`,
        method: 'GET',
        signal,
      }),
    enabled,
  })
}

export function useUpdateNotificationPreferences(userId: ComputedRef<string>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateNotificationPreferences) =>
      apiRequest<void>({
        url: `/users/${userId.value}/notification-preferences`,
        method: 'PATCH',
        data,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKey(userId.value) }),
  })
}
