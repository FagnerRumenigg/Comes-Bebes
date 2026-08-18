import { ref } from 'vue'

import { deleteDraft, listDrafts, type PublicationDraft } from './drafts'

export function useDraftsList() {
  const drafts = ref<PublicationDraft[]>([])
  const loading = ref(true)

  async function refresh(): Promise<void> {
    loading.value = true
    try {
      drafts.value = await listDrafts()
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string): Promise<void> {
    await deleteDraft(id)
    drafts.value = drafts.value.filter((draft) => draft.id !== id)
  }

  return { drafts, loading, refresh, remove }
}
