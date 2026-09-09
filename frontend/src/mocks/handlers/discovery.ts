import { delay, http, HttpResponse } from 'msw'

import type { PageResponsePublicationResponse, UserResponse } from '@/api/generated/models'
import type { NotificationItem } from '@/features/notifications/notifications'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { mockAccounts } from '@/mocks/fixtures/auth'
import { mockPublications } from '@/mocks/fixtures/publications'
import { personalizePublication, savedPublications } from '@/mocks/state/publications'
import { isFollowingMock } from '@/mocks/state/follows'

const account = mockAccounts[0]
const page = <T>(content: T[], requestedPage = 1, size = 20) => ({
  content,
  page: requestedPage,
  size,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

// Tipos reais do CHECK de application.user_notifications (ver migration V34) —
// os antigos PUBLICATION_APPROVED/PUBLICATION_REPORTED não existem mais desde
// a tela 12 (avisos). Mutável de propósito: os handlers de marcar-lido/apagar
// abaixo alteram esse array direto, sem persistir entre reloads (mock só dura
// a sessão do navegador).
const mockNotifications: NotificationItem[] = [
  {
    id: 'notification-1',
    type: 'SAVED_YOUR_PUBLICATION',
    moderationCaseId: null,
    publicationId: mockPublications[1].id,
    collectionId: null,
    actorId: mockAccounts[1].userId,
    actorDisplayName: mockAccounts[1].displayName,
    actorAvatarKey: null,
    publicationTitle: mockPublications[1].title,
    publicationImageUrl: mockPublications[1].imageUrl,
    collectionName: null,
    createdAt: '2026-08-08T13:00:00-03:00',
    readAt: null,
  },
  {
    id: 'notification-2',
    type: 'REACTED_TO_YOUR_PUBLICATION',
    moderationCaseId: null,
    publicationId: mockPublications[0].id,
    collectionId: null,
    actorId: mockPublications[0].authorId,
    actorDisplayName: mockPublications[0].authorDisplayName,
    actorAvatarKey: null,
    publicationTitle: mockPublications[0].title,
    publicationImageUrl: mockPublications[0].imageUrl,
    collectionName: null,
    createdAt: '2026-08-07T09:00:00-03:00',
    readAt: '2026-08-07T12:00:00-03:00',
  },
]

export const discoveryMockHandlers = [
  http.get('*/u/:username', async ({ params, request }) => {
    await delay(180)
    const found = mockAccounts.find((item) => item.username === params.username)
    if (!found) return HttpResponse.json({ message: 'Perfil não encontrado.' }, { status: 404 })
    const viewerUsername = mockAuthenticatedUsername(request)
    const response: UserResponse = {
      id: found.userId,
      username: found.username,
      displayName: found.displayName,
      role: found.role,
      status: 'ACTIVE',
      onboardingCompleted: true,
      bio: null,
      avatarKey: null,
      followedByCurrentUser:
        !viewerUsername || viewerUsername === found.username
          ? null
          : isFollowingMock(viewerUsername, found.userId),
    }
    return HttpResponse.json(response)
  }),
  http.get('*/users/:id/publications', async ({ params, request }) => {
    await delay(180)
    if (params.id !== account.userId) return HttpResponse.json(page([]))
    const url = new URL(request.url)
    const username = mockAuthenticatedUsername(request)
    const authenticated = Boolean(username)
    const content = (
      params.id === account.userId
        ? mockPublications.slice(0, 2)
        : mockPublications.filter((item) => item.authorId === params.id)
    )
      .filter((item) => item.visibility === 'PUBLIC' || authenticated)
      .map((item) => personalizePublication(item, username))
    return HttpResponse.json(
      page(
        content,
        Number(url.searchParams.get('page') ?? 1),
        Number(url.searchParams.get('size') ?? 20),
      ) satisfies PageResponsePublicationResponse,
    )
  }),
  http.get('*/publications/search', async ({ request }) => {
    await delay(180)
    const url = new URL(request.url)
    const title = (url.searchParams.get('title') ?? '').toLowerCase()
    const ingredient = (url.searchParams.get('ingredient') ?? '').toLowerCase()
    const username = mockAuthenticatedUsername(request)
    const authenticated = Boolean(username)
    const content = mockPublications
      .filter(
        (item) =>
          (item.visibility === 'PUBLIC' || authenticated) &&
          ((!title && !ingredient) ||
            (title && item.title?.toLowerCase().includes(title)) ||
            (ingredient &&
              item.recipePreview?.ingredients.some((entry) =>
                entry.name.toLowerCase().includes(ingredient),
              ))),
      )
      .map((item) => personalizePublication(item, username))
    return HttpResponse.json(page(content) satisfies PageResponsePublicationResponse)
  }),
  http.get('*/publications/saved', async ({ request }) => {
    await delay(180)
    const username = mockAuthenticatedUsername(request)
    if (!username)
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    return HttpResponse.json(page(savedPublications(username)))
  }),
  http.get('*/users/:id/notifications', async ({ params, request }) => {
    await delay(180)
    if (!request.headers.has('authorization') || params.id !== account.userId)
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    return HttpResponse.json(page(mockNotifications))
  }),
  http.patch('*/users/:id/notifications/read', async ({ params, request }) => {
    await delay(120)
    if (!request.headers.has('authorization') || params.id !== account.userId)
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    mockNotifications.forEach((item) => {
      if (!item.readAt) item.readAt = new Date().toISOString()
    })
    return new HttpResponse(null, { status: 204 })
  }),
  http.delete('*/users/:id/notifications/:notificationId', async ({ params, request }) => {
    await delay(120)
    if (!request.headers.has('authorization') || params.id !== account.userId)
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const index = mockNotifications.findIndex((item) => item.id === params.notificationId)
    if (index === -1)
      return HttpResponse.json({ message: 'Aviso não encontrado.' }, { status: 404 })
    mockNotifications.splice(index, 1)
    return new HttpResponse(null, { status: 204 })
  }),
  http.delete('*/users/:id/notifications', async ({ params, request }) => {
    await delay(120)
    if (!request.headers.has('authorization') || params.id !== account.userId)
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    mockNotifications.length = 0
    return new HttpResponse(null, { status: 204 })
  }),
]
