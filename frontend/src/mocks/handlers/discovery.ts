import { delay, http, HttpResponse } from 'msw'

import type {
  PageResponseNotificationResponse,
  PageResponsePublicationResponse,
  UserResponse,
} from '@/api/generated/models'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { mockAccounts } from '@/mocks/fixtures/auth'
import { mockPublications } from '@/mocks/fixtures/publications'
import { personalizePublication, savedPublications } from '@/mocks/state/publications'
import { followersCountMock, followingCountMock, isFollowingMock } from '@/mocks/state/follows'

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
      showReactionCounts: true,
      onboardingCompleted: true,
      followersCount: followersCountMock(found.userId),
      followingCount: followingCountMock(found.username),
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
    const response: PageResponseNotificationResponse = page([
      {
        id: 'notification-1',
        type: 'PUBLICATION_APPROVED',
        moderationCaseId: 'case-1',
        publicationId: mockPublications[1].id,
        actorId: null,
        readAt: null,
      },
      {
        id: 'notification-2',
        type: 'PUBLICATION_REPORTED',
        moderationCaseId: 'case-2',
        publicationId: mockPublications[0].id,
        actorId: null,
        readAt: '2026-08-07T12:00:00Z',
      },
    ])
    return HttpResponse.json(response)
  }),
]
