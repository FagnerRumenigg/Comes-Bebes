import { delay, http, HttpResponse } from 'msw'

import type {
  CreateMyVersionUploadRequest,
  CreatePublicationUploadRequest,
  MarkPublicationsViewedRequest,
  PageResponsePublicationResponse,
  PublicationResponse,
  ReactionRequest,
} from '@/api/generated/models'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { mockAccounts } from '@/mocks/fixtures/auth'
import { mockPublications } from '@/mocks/fixtures/publications'
import {
  addMockPublication,
  personalizePublication,
  reportPublication,
  setPublicationReaction,
  setPublicationSaved,
  setPublicationsViewed,
} from '@/mocks/state/publications'
import { resolveMockTags } from '@/mocks/state/tags'

const mockAuthor = mockAccounts[0]!

function authorFor(request: Request): (typeof mockAccounts)[number] {
  const username = mockAuthenticatedUsername(request)
  return mockAccounts.find((account) => account.username === username) ?? mockAuthor
}

function unauthorized(): HttpResponse<{ message: string }> {
  return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
}

async function multipartData<T>(request: Request): Promise<T> {
  const formData = await request.formData()
  const data = formData.get('data')
  const serialized = typeof data === 'string' ? data : data ? await data.text() : '{}'
  return JSON.parse(serialized) as T
}

function recipePreview(
  publicationId: string,
  recipe: CreateMyVersionUploadRequest['recipe'],
): NonNullable<PublicationResponse['recipePreview']> {
  return {
    publicationId,
    yieldQuantity: recipe.yieldQuantity ?? null,
    yieldUnit: recipe.yieldUnit ?? null,
    instructions: recipe.instructions,
    ingredients: recipe.ingredients.map((ingredient) => ({
      position: ingredient.position,
      name: ingredient.name,
      quantity: ingredient.quantity ?? null,
      unit: ingredient.unit ?? null,
      note: ingredient.note ?? null,
    })),
  }
}

export const feedMockHandlers = [
  http.get('*/publications/feed', async ({ request }) => {
    await delay(350)

    const url = new URL(request.url)
    const page = Math.max(Number(url.searchParams.get('page') ?? 1), 1)
    const size = Math.min(Math.max(Number(url.searchParams.get('size') ?? 20), 1), 50)
    const username = mockAuthenticatedUsername(request)
    const authenticated = Boolean(username)
    const visiblePublications = mockPublications.filter(
      (publication) => publication.visibility === 'PUBLIC' || authenticated,
    )
    const start = (page - 1) * size
    const content = visiblePublications
      .slice(start, start + size)
      .map((publication) => personalizePublication(publication, username))
    const totalPages = Math.max(Math.ceil(visiblePublications.length / size), 1)

    const response: PageResponsePublicationResponse = {
      content,
      page,
      size,
      totalElements: visiblePublications.length,
      totalPages,
      first: page === 1,
      last: page >= totalPages,
    }

    return HttpResponse.json(response)
  }),
  http.get('*/publications/:id', async ({ params, request }) => {
    await delay(180)
    const publication = mockPublications.find((item) => item.id === params.id)
    if (!publication)
      return HttpResponse.json({ message: 'Publicação não encontrada.' }, { status: 404 })
    const username = mockAuthenticatedUsername(request)
    if (publication.visibility === 'INTERNAL' && !username) {
      return HttpResponse.json({ message: 'Publicação não encontrada.' }, { status: 404 })
    }
    return HttpResponse.json(personalizePublication(publication, username))
  }),
  http.get('*/publications/:id/recipe', async ({ params, request }) => {
    await delay(180)
    const publication = mockPublications.find((item) => item.id === params.id)
    if (!publication?.recipePreview)
      return HttpResponse.json({ message: 'Receita não encontrada.' }, { status: 404 })
    if (publication.visibility === 'INTERNAL' && !request.headers.has('authorization')) {
      return HttpResponse.json({ message: 'Receita não encontrada.' }, { status: 404 })
    }
    return HttpResponse.json(publication.recipePreview)
  }),
  http.put('*/publications/:id/saved', async ({ params, request }) => {
    await delay(120)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    setPublicationSaved(username, String(params.id), true)
    return new HttpResponse(null, { status: 204 })
  }),
  http.delete('*/publications/:id/saved', async ({ params, request }) => {
    await delay(120)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    setPublicationSaved(username, String(params.id), false)
    return new HttpResponse(null, { status: 204 })
  }),
  http.put('*/publications/:id/reactions', async ({ params, request }) => {
    await delay(120)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    const publication = mockPublications.find((item) => item.id === params.id)
    if (!publication) {
      return HttpResponse.json({ message: 'Publicação não encontrada.' }, { status: 404 })
    }
    if (publication.authorId === authorFor(request).userId) {
      return HttpResponse.json(
        { code: 'OWN_PUBLICATION_REACTION', message: 'Você não pode reagir à sua publicação.' },
        { status: 400 },
      )
    }
    const body = (await request.json()) as ReactionRequest
    setPublicationReaction(username, String(params.id), body.reactionCode, true)
    return new HttpResponse(null, { status: 204 })
  }),
  http.delete('*/publications/:id/reactions', async ({ params, request }) => {
    await delay(120)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    const body = (await request.json()) as ReactionRequest
    setPublicationReaction(username, String(params.id), body.reactionCode, false)
    return new HttpResponse(null, { status: 204 })
  }),
  http.post('*/publications/:id/reports', async ({ params, request }) => {
    await delay(120)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    const publication = mockPublications.find((item) => item.id === params.id)
    const author = authorFor(request)
    if (!publication) {
      return HttpResponse.json({ message: 'Publicação não encontrada.' }, { status: 404 })
    }
    if (publication?.authorId === author.userId) {
      return HttpResponse.json(
        { code: 'OWN_PUBLICATION_REPORT', message: 'Você não pode denunciar sua publicação.' },
        { status: 400 },
      )
    }
    if (!reportPublication(username, String(params.id))) {
      return HttpResponse.json(
        { code: 'REPORT_ALREADY_EXISTS', message: 'Você já denunciou esta publicação.' },
        { status: 409 },
      )
    }
    return new HttpResponse(null, { status: 204 })
  }),
  http.post('*/publications/:id/my-versions', async ({ params, request }) => {
    await delay(220)
    if (!request.headers.has('authorization')) return unauthorized()
    const source = mockPublications.find((item) => item.id === params.id)
    if (!source?.recipePreview) {
      return HttpResponse.json({ message: 'Receita não encontrada.' }, { status: 404 })
    }

    const data = await multipartData<CreateMyVersionUploadRequest>(request)
    const author = authorFor(request)
    const id = crypto.randomUUID()
    const publication: PublicationResponse = {
      id,
      authorId: author.userId,
      type: 'MY_VERSION',
      visibility: data.visibility === 'INTERNAL' ? 'INTERNAL' : 'PUBLIC',
      title: `${source.title ?? 'Receita'} — ${data.titleSuffix}`,
      description: data.changeSummary || null,
      status: 'PENDING_VALIDATION',
      publishedAt: new Date().toISOString(),
      photoTakenAt: null,
      imageUrl: source.imageUrl,
      authorUsername: author.username,
      authorDisplayName: author.displayName,
      showReactionCounts: true,
      reactionTotals: {},
      selectedReactions: [],
      saved: false,
      viewedByCurrentUser: false,
      versionsCount: 0,
      originalPublicationId: source.id,
      reportedByCurrentUser: false,
      recipePreview: recipePreview(id, data.recipe),
      editedByAdmin: false,
      tags: resolveMockTags(data.tags),
    }
    source.versionsCount += 1
    addMockPublication(publication)
    return HttpResponse.json(publication, { status: 201 })
  }),
  http.post('*/publications', async ({ request }) => {
    await delay(220)
    if (!request.headers.has('authorization')) return unauthorized()
    const data = await multipartData<CreatePublicationUploadRequest>(request)
    const author = authorFor(request)
    const id = crypto.randomUUID()
    const publication: PublicationResponse = {
      id,
      authorId: author.userId,
      type: data.type === 'RECIPE' ? 'RECIPE' : 'DISH',
      visibility: data.visibility === 'INTERNAL' ? 'INTERNAL' : 'PUBLIC',
      title: data.title || null,
      description: data.description || null,
      status: 'PENDING_VALIDATION',
      publishedAt: new Date().toISOString(),
      photoTakenAt: null,
      imageUrl: mockPublications[0]!.imageUrl,
      authorUsername: author.username,
      authorDisplayName: author.displayName,
      showReactionCounts: true,
      reactionTotals: {},
      selectedReactions: [],
      saved: false,
      viewedByCurrentUser: false,
      versionsCount: 0,
      originalPublicationId: null,
      reportedByCurrentUser: false,
      recipePreview: data.recipe ? recipePreview(id, data.recipe) : null,
      editedByAdmin: false,
      tags: resolveMockTags(data.tags),
    }
    addMockPublication(publication)
    return HttpResponse.json(publication, { status: 201 })
  }),
  http.post('*/publications/views', async ({ request }) => {
    await delay(60)
    const username = mockAuthenticatedUsername(request)
    if (!username) return unauthorized()
    const data = (await request.json()) as MarkPublicationsViewedRequest
    setPublicationsViewed(username, data.publicationIds)
    return new HttpResponse(null, { status: 204 })
  }),
]
