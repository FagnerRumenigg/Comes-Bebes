import { HttpResponse, http } from 'msw'

import type { NotificationPreferencesResponse, UpdateNotificationPreferencesRequest } from '@/api/generated/models'

// Estado simples em memória (não precisa sobreviver a reload como o de follows.ts —
// nenhum teste hoje verifica persistência dessa preferência entre sessões).
const preferenceByUserId = new Map<string, boolean>()

export const notificationPreferencesMockHandlers = [
  http.get('*/users/:id/notification-preferences', ({ params }) =>
    HttpResponse.json<NotificationPreferencesResponse>({
      notifyOnFollowedPublish: preferenceByUserId.get(params.id as string) ?? true,
    }),
  ),
  http.patch('*/users/:id/notification-preferences', async ({ params, request }) => {
    const body = (await request.json()) as UpdateNotificationPreferencesRequest
    preferenceByUserId.set(params.id as string, body.notifyOnFollowedPublish ?? true)
    return new HttpResponse(null, { status: 204 })
  }),
]
