import { delay, http, HttpResponse } from 'msw'

import type { DecideModerationCaseRequest, ModerationCaseResponse } from '@/api/generated/models'
import { mockPublications } from '@/mocks/fixtures/publications'
import { setPublicationStatus } from '@/mocks/state/publications'

const cases: ModerationCaseResponse[] = [
  {
    id: 'case-1',
    publicationId: mockPublications[0].id,
    status: 'PENDING',
    reportCountAtOpen: 3,
    openedAt: '2026-08-08T09:00:00Z',
    reviewedBy: null,
    reviewedAt: null,
    decisionNote: null,
  },
  {
    id: 'case-2',
    publicationId: mockPublications[1].id,
    status: 'PENDING',
    reportCountAtOpen: 5,
    openedAt: '2026-08-08T11:00:00Z',
    reviewedBy: null,
    reviewedAt: null,
    decisionNote: null,
  },
]

function isAdmin(request: Request): boolean {
  return request.headers.get('authorization')?.includes('mock-access-admin') ?? false
}
function denied(): Response {
  return HttpResponse.json({ message: 'Acesso administrativo necessário.' }, { status: 403 })
}

export const moderationMockHandlers = [
  http.get('*/moderation/cases', async ({ request }) => {
    await delay(180)
    if (!isAdmin(request)) return denied()
    return HttpResponse.json(cases.filter((item) => item.status === 'PENDING'))
  }),
  http.get('*/moderation/cases/:id', async ({ params, request }) => {
    await delay(180)
    if (!isAdmin(request)) return denied()
    const item = cases.find((entry) => entry.id === params.id)
    return item
      ? HttpResponse.json(item)
      : HttpResponse.json({ message: 'Caso não encontrado.' }, { status: 404 })
  }),
  http.patch('*/moderation/cases/:id', async ({ params, request }) => {
    await delay(180)
    if (!isAdmin(request)) return denied()
    const body = (await request.json()) as DecideModerationCaseRequest
    if ((body.decision === 'HIDDEN' || body.decision === 'REMOVED') && !body.decisionNote?.trim())
      return HttpResponse.json({ message: 'Justificativa obrigatória.' }, { status: 400 })
    const item = cases.find((entry) => entry.id === params.id)
    if (!item) return HttpResponse.json({ message: 'Caso não encontrado.' }, { status: 404 })
    item.status = body.decision
    item.reviewedBy = 'b5d94b7e-3c45-4d28-99fe-70b96065b6c4'
    item.reviewedAt = new Date().toISOString()
    item.decisionNote = body.decisionNote ?? null
    setPublicationStatus(item.publicationId, body.decision === 'KEPT' ? 'ACTIVE' : body.decision)
    return HttpResponse.json(item)
  }),
]
