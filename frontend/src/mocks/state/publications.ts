import type { PublicationResponse, ReactionRequestReactionCode } from '@/api/generated/models'
import { mockPublications } from '@/mocks/fixtures/publications'

const MOCK_STATE_STORAGE_KEY = 'comes-e-bebes:mocks:publication-state'

const savedAtByUser = new Map<string, Map<string, string>>([
  [
    'fagner',
    new Map([
      [mockPublications[1]!.id, '2026-08-08T14:00:00Z'],
      [mockPublications[0]!.id, '2026-08-07T14:00:00Z'],
    ]),
  ],
])

const reactionsByUser = new Map<string, Map<string, Set<ReactionRequestReactionCode>>>([
  [
    'fagner',
    new Map([[mockPublications[1]!.id, new Set<ReactionRequestReactionCode>(['WANT_TO_MAKE'])]]),
  ],
])

const reportsByUser = new Map<string, Set<string>>()

interface SerializedPublicationState {
  saved: Array<[string, Array<[string, string]>]>
  reactions: Array<[string, Array<[string, ReactionRequestReactionCode[]]>]>
  reports: Array<[string, string[]]>
  publications: PublicationResponse[]
}

function restoreState(): void {
  if (typeof window === 'undefined') return
  try {
    const value = window.localStorage.getItem(MOCK_STATE_STORAGE_KEY)
    if (!value) return
    const state = JSON.parse(value) as SerializedPublicationState
    if (
      !Array.isArray(state.saved) ||
      !Array.isArray(state.reactions) ||
      !Array.isArray(state.reports) ||
      !Array.isArray(state.publications)
    ) {
      throw new Error('Estado mockado incompatível.')
    }

    savedAtByUser.clear()
    for (const [username, saved] of state.saved) {
      savedAtByUser.set(username, new Map(saved))
    }
    reactionsByUser.clear()
    for (const [username, reactions] of state.reactions) {
      reactionsByUser.set(
        username,
        new Map(reactions.map(([publicationId, codes]) => [publicationId, new Set(codes)])),
      )
    }
    reportsByUser.clear()
    for (const [username, reports] of state.reports) {
      reportsByUser.set(username, new Set(reports))
    }
    mockPublications.splice(0, mockPublications.length, ...state.publications)
  } catch {
    window.localStorage.removeItem(MOCK_STATE_STORAGE_KEY)
  }
}

function persistState(): void {
  if (typeof window === 'undefined') return
  const state: SerializedPublicationState = {
    saved: [...savedAtByUser].map(([username, saved]) => [username, [...saved]]),
    reactions: [...reactionsByUser].map(([username, reactions]) => [
      username,
      [...reactions].map(([publicationId, codes]) => [publicationId, [...codes]]),
    ]),
    reports: [...reportsByUser].map(([username, reports]) => [username, [...reports]]),
    publications: mockPublications,
  }
  window.localStorage.setItem(MOCK_STATE_STORAGE_KEY, JSON.stringify(state))
}

restoreState()

function savedFor(username: string): Map<string, string> {
  const existing = savedAtByUser.get(username)
  if (existing) return existing
  const created = new Map<string, string>()
  savedAtByUser.set(username, created)
  return created
}

function reactionsFor(username: string): Map<string, Set<ReactionRequestReactionCode>> {
  const existing = reactionsByUser.get(username)
  if (existing) return existing
  const created = new Map<string, Set<ReactionRequestReactionCode>>()
  reactionsByUser.set(username, created)
  return created
}

export function personalizePublication(
  publication: PublicationResponse,
  username: string | null,
): PublicationResponse {
  if (!username) {
    return {
      ...publication,
      saved: false,
      selectedReactions: [],
      reportedByCurrentUser: false,
    }
  }

  return {
    ...publication,
    saved: savedFor(username).has(publication.id),
    selectedReactions: [...(reactionsFor(username).get(publication.id) ?? [])],
    reportedByCurrentUser: reportsByUser.get(username)?.has(publication.id) ?? false,
  }
}

export function savedPublications(
  username: string,
): Array<{ publicationId: string; savedAt: string }> {
  return [...savedFor(username)].map(([publicationId, savedAt]) => ({ publicationId, savedAt }))
}

export function setPublicationSaved(username: string, publicationId: string, saved: boolean): void {
  const savedPublications = savedFor(username)
  if (saved) {
    savedPublications.set(publicationId, new Date().toISOString())
  } else {
    savedPublications.delete(publicationId)
  }
  persistState()
}

export function setPublicationReaction(
  username: string,
  publicationId: string,
  reactionCode: ReactionRequestReactionCode,
  active: boolean,
): void {
  const publication = mockPublications.find((item) => item.id === publicationId)
  const reactions = reactionsFor(username)
  const selected = reactions.get(publicationId) ?? new Set<ReactionRequestReactionCode>()
  const wasActive = selected.has(reactionCode)

  if (active) selected.add(reactionCode)
  else selected.delete(reactionCode)

  reactions.set(publicationId, selected)
  if (!publication || wasActive === active) return

  const currentTotal = publication.reactionTotals?.[reactionCode] ?? 0
  publication.reactionTotals = {
    ...publication.reactionTotals,
    [reactionCode]: Math.max(currentTotal + (active ? 1 : -1), 0),
  }
  persistState()
}

export function reportPublication(username: string, publicationId: string): boolean {
  const reported = reportsByUser.get(username) ?? new Set<string>()
  if (reported.has(publicationId)) return false
  reported.add(publicationId)
  reportsByUser.set(username, reported)
  persistState()
  return true
}

export function addMockPublication(publication: PublicationResponse): void {
  mockPublications.unshift(publication)
  persistState()
}

export function setPublicationStatus(
  publicationId: string,
  status: PublicationResponse['status'],
): void {
  const publication = mockPublications.find((item) => item.id === publicationId)
  if (!publication) return
  publication.status = status
  persistState()
}
