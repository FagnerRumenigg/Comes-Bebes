<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { removeSaved, save } from '@/api/generated/publications/publications'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{
  publicationId: string
  saved: boolean
}>()

const authStore = useAuthStore()
const queryClient = useQueryClient()
const isSaved = ref(props.saved)
const errorMessage = ref<string | null>(null)

watch(
  () => props.saved,
  (value) => {
    isSaved.value = value
  },
)

const mutation = useMutation({
  mutationFn: (nextSaved: boolean) =>
    nextSaved ? save(props.publicationId, {}) : removeSaved(props.publicationId, {}),
  onSuccess: () => {
    void queryClient.invalidateQueries({ queryKey: ['publications'] })
  },
  onError: (error, nextSaved) => {
    isSaved.value = !nextSaved
    errorMessage.value = normalizeHttpError(error).message
  },
})

function toggle(): void {
  if (!authStore.authenticated || mutation.isPending.value) return
  errorMessage.value = null
  const nextSaved = !isSaved.value
  isSaved.value = nextSaved
  mutation.mutate(nextSaved)
}
</script>

<template>
  <div class="save-button">
    <button
      v-if="!authStore.authenticated"
      class="save-button__control"
      type="button"
      aria-label="Salvar publicação; é necessário entrar"
      @click="showAuthNotice"
    >
      Salvar
    </button>
    <button
      v-else
      class="save-button__control"
      type="button"
      :aria-pressed="isSaved"
      :aria-label="isSaved ? 'Remover dos salvos' : 'Salvar publicação'"
      :disabled="mutation.isPending.value"
      @click="toggle"
    >
      {{ isSaved ? 'Salvo' : 'Salvar' }}
    </button>
    <span v-if="errorMessage" class="save-button__error" role="alert">{{ errorMessage }}</span>
  </div>
</template>

<style scoped>
.save-button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}

.save-button__control {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.save-button__control[aria-pressed='true'] {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  border-color: var(--color-primary);
}

.save-button__control:disabled {
  opacity: 0.6;
}

.save-button__error {
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}
</style>
