import { mockAccounts } from '@/mocks/fixtures/auth'

export interface MockCollection {
  id: string
  authorId: string
  name: string
  description: string | null
  visibility: 'PUBLIC' | 'PRIVATE'
  createdAt: string
  updatedAt: string
}

const MOCK_STATE_STORAGE_KEY = 'comes-e-bebes:mocks:collection-state'

const collectionsById = new Map<string, MockCollection>()
const publicationIdsByCollectionId = new Map<string, string[]>()
const followersByCollectionId = new Map<string, Set<string>>()

function seed(): void {
  const author = mockAccounts[0]!
  const id = 'mock-collection-1'
  collectionsById.set(id, {
    id,
    authorId: author.userId,
    name: 'Receitas de domingo',
    description: 'Pratos para reunir a família',
    visibility: 'PUBLIC',
    createdAt: '2026-08-01T12:00:00Z',
    updatedAt: '2026-08-01T12:00:00Z',
  })
  publicationIdsByCollectionId.set(id, [])
}

interface SerializedCollectionState {
  collections: MockCollection[]
  publicationIds: Array<[string, string[]]>
  followers: Array<[string, string[]]>
}

function restoreState(): void {
  seed()
  if (typeof window === 'undefined') return
  try {
    const value = window.localStorage.getItem(MOCK_STATE_STORAGE_KEY)
    if (!value) return
    const state = JSON.parse(value) as SerializedCollectionState
    if (
      !Array.isArray(state.collections) ||
      !Array.isArray(state.publicationIds) ||
      !Array.isArray(state.followers)
    ) {
      throw new Error('Estado mockado incompatível.')
    }
    collectionsById.clear()
    state.collections.forEach((collection) => collectionsById.set(collection.id, collection))
    publicationIdsByCollectionId.clear()
    state.publicationIds.forEach(([id, ids]) => publicationIdsByCollectionId.set(id, ids))
    followersByCollectionId.clear()
    state.followers.forEach(([id, ids]) => followersByCollectionId.set(id, new Set(ids)))
  } catch {
    window.localStorage.removeItem(MOCK_STATE_STORAGE_KEY)
  }
}

function persistState(): void {
  if (typeof window === 'undefined') return
  const state: SerializedCollectionState = {
    collections: [...collectionsById.values()],
    publicationIds: [...publicationIdsByCollectionId].map(([id, ids]) => [id, [...ids]]),
    followers: [...followersByCollectionId].map(([id, set]) => [id, [...set]]),
  }
  window.localStorage.setItem(MOCK_STATE_STORAGE_KEY, JSON.stringify(state))
}

restoreState()

export function createMockCollection(
  authorId: string,
  name: string,
  description: string | null,
  visibility: 'PUBLIC' | 'PRIVATE',
): MockCollection {
  const now = new Date().toISOString()
  const collection: MockCollection = {
    id: `mock-collection-${crypto.randomUUID()}`,
    authorId,
    name,
    description,
    visibility,
    createdAt: now,
    updatedAt: now,
  }
  collectionsById.set(collection.id, collection)
  publicationIdsByCollectionId.set(collection.id, [])
  persistState()
  return collection
}

export function getMockCollection(id: string): MockCollection | undefined {
  return collectionsById.get(id)
}

export function updateMockCollection(
  id: string,
  name: string,
  description: string | null,
  visibility: 'PUBLIC' | 'PRIVATE',
): MockCollection | undefined {
  const collection = collectionsById.get(id)
  if (!collection) return undefined
  collection.name = name
  collection.description = description
  collection.visibility = visibility
  collection.updatedAt = new Date().toISOString()
  persistState()
  return collection
}

export function deleteMockCollection(id: string): void {
  collectionsById.delete(id)
  publicationIdsByCollectionId.delete(id)
  followersByCollectionId.delete(id)
  persistState()
}

export function listMockCollectionsByAuthor(authorId: string): MockCollection[] {
  return [...collectionsById.values()].filter((collection) => collection.authorId === authorId)
}

export function addMockCollectionPublication(collectionId: string, publicationId: string): void {
  const ids = publicationIdsByCollectionId.get(collectionId) ?? []
  if (!ids.includes(publicationId)) ids.push(publicationId)
  publicationIdsByCollectionId.set(collectionId, ids)
  persistState()
}

export function removeMockCollectionPublication(collectionId: string, publicationId: string): void {
  const ids = publicationIdsByCollectionId.get(collectionId) ?? []
  publicationIdsByCollectionId.set(
    collectionId,
    ids.filter((id) => id !== publicationId),
  )
  persistState()
}

export function mockCollectionPublicationIds(collectionId: string): string[] {
  return publicationIdsByCollectionId.get(collectionId) ?? []
}

export function setMockCollectionFollowing(
  userId: string,
  collectionId: string,
  following: boolean,
): void {
  const set = followersByCollectionId.get(collectionId) ?? new Set<string>()
  if (following) set.add(userId)
  else set.delete(userId)
  followersByCollectionId.set(collectionId, set)
  persistState()
}

export function isMockCollectionFollowing(userId: string, collectionId: string): boolean {
  return followersByCollectionId.get(collectionId)?.has(userId) ?? false
}

export function mockCollectionFollowersCount(collectionId: string): number {
  return followersByCollectionId.get(collectionId)?.size ?? 0
}

export function listMockFollowedCollections(userId: string): MockCollection[] {
  return [...collectionsById.values()].filter((collection) =>
    followersByCollectionId.get(collection.id)?.has(userId),
  )
}
