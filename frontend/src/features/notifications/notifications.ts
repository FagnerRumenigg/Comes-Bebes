import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, type ComputedRef } from 'vue'

import { apiRequest } from '@/api/client'
import { getNotificationsQueryKey } from '@/api/generated/users/users'
import { formatRelativeTime } from '@/utils/relativeTime'

/**
 * GET /users/{id}/notifications ganhou campos novos (collectionId,
 * actorDisplayName, publicationTitle, publicationImageUrl, collectionName,
 * createdAt — docs/telas/12-avisos.html) que o client gerado ainda não conhece
 * (orval não roda sem subir o backend). Reaproveita a mesma query key do hook
 * gerado (getNotificationsQueryKey) pra compartilhar cache com
 * useUnreadNotificationsCount, mas chama o endpoint direto com o tipo certo.
 * Trocar por um hook gerado depois de rodar `npm run api:generate`.
 */
export interface NotificationItem {
  id: string
  type: string
  moderationCaseId: string | null
  publicationId: string | null
  collectionId: string | null
  actorId: string | null
  actorDisplayName: string | null
  publicationTitle: string | null
  publicationImageUrl: string | null
  collectionName: string | null
  createdAt: string
  readAt: string | null
}

interface NotificationsPage {
  content: NotificationItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export const NOTIFICATIONS_PAGE_SIZE = 50
const NOTIFICATIONS_PARAMS = { page: 1, size: NOTIFICATIONS_PAGE_SIZE }

export function useNotificationsList(userId: ComputedRef<string>, enabled: ComputedRef<boolean>) {
  return useQuery({
    queryKey: computed(() => getNotificationsQueryKey(userId.value, NOTIFICATIONS_PARAMS)),
    queryFn: ({ signal }) =>
      apiRequest<NotificationsPage>({
        url: `/users/${userId.value}/notifications`,
        method: 'GET',
        params: NOTIFICATIONS_PARAMS,
        signal,
      }),
    enabled,
  })
}

export function useMarkNotificationsRead(userId: ComputedRef<string>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      apiRequest<void>({ url: `/users/${userId.value}/notifications/read`, method: 'PATCH' }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['users', userId.value, 'notifications'] }),
  })
}

export function useDeleteNotification(userId: ComputedRef<string>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (notificationId: string) =>
      apiRequest<void>({
        url: `/users/${userId.value}/notifications/${notificationId}`,
        method: 'DELETE',
      }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['users', userId.value, 'notifications'] }),
  })
}

export function useClearNotifications(userId: ComputedRef<string>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      apiRequest<void>({ url: `/users/${userId.value}/notifications`, method: 'DELETE' }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['users', userId.value, 'notifications'] }),
  })
}

export interface NotificationDayGroup {
  label: string
  items: NotificationItem[]
}

const WEEKDAYS = [
  'domingo',
  'segunda-feira',
  'terça-feira',
  'quarta-feira',
  'quinta-feira',
  'sexta-feira',
  'sábado',
]

function daysAgo(isoDate: string): number {
  const date = new Date(isoDate)
  const now = new Date()
  const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((startOfToday.getTime() - startOfDate.getTime()) / 86_400_000)
}

/** "Hoje" / "Ontem" / dia da semana / data — mesmos rótulos de docs/telas/12-avisos.html. */
export function dayGroupLabel(isoDate: string): string {
  const diff = daysAgo(isoDate)
  if (diff <= 0) return 'Hoje'
  if (diff === 1) return 'Ontem'
  if (diff < 7) return WEEKDAYS[new Date(isoDate).getDay()]
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long' }).format(new Date(isoDate))
}

/** Presume os avisos já ordenados do mais recente pro mais antigo (o backend ordena por createdAt desc). */
export function groupNotificationsByDay(items: NotificationItem[]): NotificationDayGroup[] {
  const groups: NotificationDayGroup[] = []
  const groupsByLabel = new Map<string, NotificationDayGroup>()
  for (const item of items) {
    const label = dayGroupLabel(item.createdAt)
    let group = groupsByLabel.get(label)
    if (!group) {
      group = { label, items: [] }
      groupsByLabel.set(label, group)
      groups.push(group)
    }
    group.items.push(item)
  }
  return groups
}

/** Texto de "quando" de cada item: relativo hoje, "ontem, às HH:mm", dia da semana, ou data. */
export function formatNotificationWhen(isoDate: string): string {
  const diff = daysAgo(isoDate)
  if (diff <= 0) return formatRelativeTime(isoDate)
  const time = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(
    new Date(isoDate),
  )
  if (diff === 1) return `ontem, às ${time}`
  if (diff < 7) return WEEKDAYS[new Date(isoDate).getDay()]
  return dayGroupLabel(isoDate)
}
