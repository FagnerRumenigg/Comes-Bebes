import { computed } from 'vue'

import { useNotifications } from '@/api/generated/users/users'
import { useAuthStore } from '@/stores/auth.store'

/**
 * Não existe endpoint dedicado de contagem — conta os não lidos da
 * primeira página. Suficiente para o badge do nav (mockup nunca mostra
 * número exato de outras pessoas, só do próprio usuário).
 */
export function useUnreadNotificationsCount() {
  const authStore = useAuthStore()
  const userId = computed(() => authStore.identity?.userId ?? '')

  const query = useNotifications(
    userId,
    { page: 1, size: 50 },
    { query: { enabled: computed(() => authStore.authenticated) } },
  )

  const count = computed(
    () => query.data.value?.content.filter((notification) => !notification.readAt).length ?? 0,
  )

  return { count }
}
