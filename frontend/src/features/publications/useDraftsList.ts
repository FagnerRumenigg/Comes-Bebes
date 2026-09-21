import { ref } from 'vue'

import { deleteDraft, listDrafts, type PublicationDraft } from './drafts'

export function useDraftsList() {
  const drafts = ref<PublicationDraft[]>([])
  const loading = ref(true)
  const error = ref(false)

  async function refresh(): Promise<void> {
    loading.value = true
    error.value = false
    try {
      drafts.value = await listDrafts()
    } catch {
      drafts.value = []
      error.value = true
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string): Promise<void> {
    await deleteDraft(id)
    drafts.value = drafts.value.filter((draft) => draft.id !== id)
  }

  return { drafts, loading, error, refresh, remove }
}
