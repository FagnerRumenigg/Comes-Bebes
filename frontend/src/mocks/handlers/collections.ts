import { delay, http, HttpResponse } from 'msw'

import type { CollectionResponse, PageResponsePublicationResponse } from '@/api/generated/models'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { mockAccounts } from '@/mocks/fixtures/auth'
import { mockPublications } from '@/mocks/fixtures/publications'
import { personalizePublication } from '@/mocks/state/publications'
import {
  addMockCollectionPublication,
  createMockCollection,
  deleteMockCollection,
  getMockCollection,
  isMockCollectionFollowing,
  listMockCollectionsByAuthor,
  listMockFollowedCollections,
  mockCollectionFollowersCount,
  mockCollectionPublicationIds,
  removeMockCollectionPublication,
  setMockCollectionFollowing,
  updateMockCollection,
  type MockCollection,
} from '@/mocks/state/collections'

const page = <T>(content: T[], requestedPage = 1, size = 20) => ({
  content,
  page: requestedPage,
  size,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
})

function accountByUserId(userId: string) {
  return mockAccounts.find((account) => account.userId === userId)
}

function accountByUsername(username: string) {
  return mockAccounts.find((account) => account.username === username)
}

function viewerIdFor(request: Request): string | null {
  const username = mockAuthenticatedUsername(request)
  return username ? (accountByUsername(username)?.userId ?? null) : null
}

function toResponse(collection: MockCollection, viewerId: string | null): CollectionResponse {
  const author = accountByUserId(collection.authorId)
  return {
    id: collection.id,
    authorId: collection.authorId,
    authorUsername: author?.username ?? '',
    authorDisplayName: author?.displayName ?? '',
    name: collection.name,
    description: collection.description,
    visibility: collection.visibility,
    publicationsCount: mockCollectionPublicationIds(collection.id).length,
    followersCount: mockCollectionFollowersCount(collection.id),
    followedByCurrentUser:
      !viewerId || viewerId === collection.authorId
        ? null
        : isMockCollectionFollowing(viewerId, collection.id),
    createdAt: collection.createdAt,
    updatedAt: collection.updatedAt,
  }
}

interface CollectionRequestBody {
  name: string
  description?: string | null
  visibility: 'PUBLIC' | 'PRIVATE'
}

export const collectionsMockHandlers = [
  http.post('*/collections', async ({ request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const body = (await request.json()) as CollectionRequestBody
    const collection = createMockCollection(
      viewerId,
      body.name,
      body.description ?? null,
      body.visibility,
    )
    return HttpResponse.json(toResponse(collection, viewerId), { status: 201 })
  }),

  http.get('*/collections/followed', async ({ request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const content = listMockFollowedCollections(viewerId).map((collection) =>
      toResponse(collection, viewerId),
    )
    return HttpResponse.json(page(content))
  }),

  http.get('*/collections/:id', async ({ params, request }) => {
    await delay(150)
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    const viewerId = viewerIdFor(request)
    if (collection.visibility === 'PRIVATE' && viewerId !== collection.authorId) {
      return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    }
    return HttpResponse.json(toResponse(collection, viewerId))
  }),

  http.patch('*/collections/:id', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    if (collection.authorId !== viewerId) {
      return HttpResponse.json({ message: 'Você não é o autor desta coleção.' }, { status: 400 })
    }
    const body = (await request.json()) as CollectionRequestBody
    const updated = updateMockCollection(
      params.id as string,
      body.name,
      body.description ?? null,
      body.visibility,
    )!
    return HttpResponse.json(toResponse(updated, viewerId))
  }),

  http.delete('*/collections/:id', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    if (collection.authorId !== viewerId) {
      return HttpResponse.json({ message: 'Você não é o autor desta coleção.' }, { status: 400 })
    }
    deleteMockCollection(params.id as string)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('*/collections/:id/publications', async ({ params, request }) => {
    await delay(150)
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    const username = mockAuthenticatedUsername(request)
    const ids = mockCollectionPublicationIds(params.id as string)
    const content = ids
      .map((publicationId) => mockPublications.find((item) => item.id === publicationId))
      .filter((item): item is NonNullable<typeof item> => Boolean(item))
      .map((item) => personalizePublication(item, username))
    return HttpResponse.json(page(content) satisfies PageResponsePublicationResponse)
  }),

  http.put('*/collections/:id/publications/:publicationId', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    if (collection.authorId !== viewerId) {
      return HttpResponse.json({ message: 'Você não é o autor desta coleção.' }, { status: 400 })
    }
    addMockCollectionPublication(params.id as string, params.publicationId as string)
    return new HttpResponse(null, { status: 204 })
  }),

  http.delete('*/collections/:id/publications/:publicationId', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    const collection = getMockCollection(params.id as string)
    if (!collection) return HttpResponse.json({ message: 'Coleção não encontrada.' }, { status: 404 })
    if (collection.authorId !== viewerId) {
      return HttpResponse.json({ message: 'Você não é o autor desta coleção.' }, { status: 400 })
    }
    removeMockCollectionPublication(params.id as string, params.publicationId as string)
    return new HttpResponse(null, { status: 204 })
  }),

  http.put('*/collections/:id/follow', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    setMockCollectionFollowing(viewerId, params.id as string, true)
    return new HttpResponse(null, { status: 204 })
  }),

  http.delete('*/collections/:id/follow', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    if (!viewerId) return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    setMockCollectionFollowing(viewerId, params.id as string, false)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('*/users/:id/collections', async ({ params, request }) => {
    await delay(150)
    const viewerId = viewerIdFor(request)
    const all = listMockCollectionsByAuthor(params.id as string)
    const visible = all.filter(
      (collection) => collection.visibility === 'PUBLIC' || collection.authorId === viewerId,
    )
    const content = visible.map((collection) => toResponse(collection, viewerId))
    return HttpResponse.json(page(content))
  }),
]
