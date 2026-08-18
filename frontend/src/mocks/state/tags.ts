import type { TagResponse } from '@/api/generated/models'

const MOCK_STATE_STORAGE_KEY = 'comes-e-bebes:mocks:tag-state'
const MAX_TAGS_PER_PUBLICATION = 5

export function slugifyMockTag(rawName: string): string {
  const lowercase = rawName.trim().toLowerCase()
  const withoutDiacritics = lowercase.normalize('NFD').replace(/\p{M}/gu, '')
  return withoutDiacritics.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
}

const tagsBySlug = new Map<string, TagResponse>([
  ['vegetariano', { name: 'Vegetariano', slug: 'vegetariano', official: true }],
  ['sem-gluten', { name: 'Sem Glúten', slug: 'sem-gluten', official: true }],
])

function restoreState(): void {
  if (typeof window === 'undefined') return
  try {
    const value = window.localStorage.getItem(MOCK_STATE_STORAGE_KEY)
    if (!value) return
    const entries = JSON.parse(value) as Array<[string, TagResponse]>
    if (!Array.isArray(entries)) throw new Error('Estado de tags mockado incompatível.')
    tagsBySlug.clear()
    for (const [slug, tag] of entries) tagsBySlug.set(slug, tag)
  } catch {
    window.localStorage.removeItem(MOCK_STATE_STORAGE_KEY)
  }
}

function persistState(): void {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(MOCK_STATE_STORAGE_KEY, JSON.stringify([...tagsBySlug]))
}

restoreState()

export function resolveMockTags(rawNames: string[] | undefined | null): TagResponse[] {
  if (!rawNames || rawNames.length === 0) return []

  const bySlug = new Map<string, string>()
  for (const rawName of rawNames.slice(0, MAX_TAGS_PER_PUBLICATION)) {
    if (!rawName?.trim()) continue
    const slug = slugifyMockTag(rawName)
    if (!slug || bySlug.has(slug)) continue
    bySlug.set(slug, rawName.trim())
  }

  const resolved: TagResponse[] = []
  let created = false
  for (const [slug, name] of bySlug) {
    let tag = tagsBySlug.get(slug)
    if (!tag) {
      tag = { name, slug, official: false }
      tagsBySlug.set(slug, tag)
      created = true
    }
    resolved.push(tag)
  }
  if (created) persistState()
  return resolved
}

export function searchMockTags(query: string): TagResponse[] {
  const slugPrefix = slugifyMockTag(query)
  if (!slugPrefix) return []
  return [...tagsBySlug.values()]
    .filter((tag) => tag.slug.startsWith(slugPrefix))
    .sort((a, b) => Number(b.official) - Number(a.official) || a.name.localeCompare(b.name))
    .slice(0, 10)
}
