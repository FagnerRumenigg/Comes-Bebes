import { HttpResponse, http } from 'msw'

import type { NotificationPreferences } from '@/features/settings/notificationPreferences'

const DEFAULT_PREFERENCES: NotificationPreferences = {
  notifyOnFollowedPublish: false,
  notifyOnSaved: true,
  notifyOnReacted: true,
  notifyOnMyVersion: true,
  notifyOnCollectionNewItem: true,
  notifyOnCollectionShared: true,
  notifyWeeklyEmail: false,
}

// Estado simples em memória (não precisa sobreviver a reload como o de follows.ts —
// nenhum teste hoje verifica persistência dessa preferência entre sessões).
const preferencesByUserId = new Map<string, NotificationPreferences>()

export const notificationPreferencesMockHandlers = [
  http.get('*/users/:id/notification-preferences', ({ params }) =>
    HttpResponse.json<NotificationPreferences>(
      preferencesByUserId.get(params.id as string) ?? DEFAULT_PREFERENCES,
    ),
  ),
  http.patch('*/users/:id/notification-preferences', async ({ params, request }) => {
    const userId = params.id as string
    const body = (await request.json()) as Partial<NotificationPreferences>
    const current = preferencesByUserId.get(userId) ?? DEFAULT_PREFERENCES
    preferencesByUserId.set(userId, { ...current, ...body })
    return new HttpResponse(null, { status: 204 })
  }),
]
