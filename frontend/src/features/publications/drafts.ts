import type { IngredientDraft } from '@/components/publication/IngredientEditor.vue'

export interface PublicationDraft {
  id: string
  mode: 'CREATE' | 'MY_VERSION'
  sourceId: string | null
  createdAt: string
  updatedAt: string
  type: 'DISH' | 'RECIPE'
  visibility: 'PUBLIC' | 'INTERNAL'
  title: string
  description: string
  titleSuffix: string
  changeSummary: string
  instructions: string
  yieldQuantity: string
  yieldUnit: string
  ingredients: IngredientDraft[]
  image: File | null
}

const DB_NAME = 'comes-e-bebes-drafts'
const DB_VERSION = 1
const STORE_NAME = 'publication_drafts'

function isIndexedDbAvailable(): boolean {
  return typeof window !== 'undefined' && 'indexedDB' in window && window.indexedDB != null
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = window.indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function listDrafts(): Promise<PublicationDraft[]> {
  if (!isIndexedDbAvailable()) return []
  const db = await openDatabase()
  return new Promise((resolve, reject) => {
    const request = db.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).getAll()
    request.onsuccess = () => {
      const drafts = (request.result as PublicationDraft[]).sort((a, b) =>
        b.updatedAt.localeCompare(a.updatedAt),
      )
      resolve(drafts)
    }
    request.onerror = () => reject(request.error)
  })
}

export async function getDraft(id: string): Promise<PublicationDraft | null> {
  if (!isIndexedDbAvailable()) return null
  const db = await openDatabase()
  return new Promise((resolve, reject) => {
    const request = db.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).get(id)
    request.onsuccess = () => resolve((request.result as PublicationDraft | undefined) ?? null)
    request.onerror = () => reject(request.error)
  })
}

export async function saveDraft(draft: PublicationDraft): Promise<void> {
  if (!isIndexedDbAvailable()) return
  const db = await openDatabase()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put(draft)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function deleteDraft(id: string): Promise<void> {
  if (!isIndexedDbAvailable()) return
  const db = await openDatabase()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export function hasDraftContent(
  draft: Pick<
    PublicationDraft,
    'title' | 'description' | 'titleSuffix' | 'changeSummary' | 'instructions' | 'ingredients' | 'image'
  >,
): boolean {
  return Boolean(
    draft.title.trim() ||
      draft.description.trim() ||
      draft.titleSuffix.trim() ||
      draft.changeSummary.trim() ||
      draft.instructions.trim() ||
      draft.ingredients.some((ingredient) => ingredient.name.trim()) ||
      draft.image,
  )
}
