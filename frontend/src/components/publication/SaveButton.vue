<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import {
  useAddCollectionPublication,
  useCreateCollection,
} from '@/api/generated/collections/collections'
import { removeSaved, save } from '@/api/generated/publications/publications'
import { getGetUserCollectionsQueryKey, useGetUserCollections } from '@/api/generated/users/users'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const TOAST_DURATION_MS = 8_000

const props = defineProps<{
  publicationId: string
  saved: boolean
}>()

const authStore = useAuthStore()
const queryClient = useQueryClient()
const userId = computed(() => authStore.identity?.userId ?? '')

const isSaved = ref(props.saved)
const errorMessage = ref<string | null>(null)
const toastOpen = ref(false)
let toastTimer: ReturnType<typeof setTimeout> | undefined

const organizeOpen = ref(false)
const newCollectionName = ref('')
const organizeError = ref('')
const addedCollectionIds = ref(new Set<string>())

const collectionsQuery = useGetUserCollections(userId, {
  page: 1,
  size: 50,
}, { query: { enabled: organizeOpen } })

const saveMutation = useMutation({
  mutationFn: () => save(props.publicationId, {}),
  onSuccess: () => {
    void queryClient.invalidateQueries({ queryKey: ['publications'] })
    isSaved.value = true
    toastOpen.value = true
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => {
      toastOpen.value = false
    }, TOAST_DURATION_MS)
  },
  onError: (error) => {
    errorMessage.value = normalizeHttpError(error).message
  },
})

const unsaveMutation = useMutation({
  mutationFn: () => removeSaved(props.publicationId, {}),
  onSuccess: () => {
    void queryClient.invalidateQueries({ queryKey: ['publications'] })
    isSaved.value = false
    organizeOpen.value = false
  },
  onError: (error) => {
    organizeError.value = normalizeHttpError(error).message
  },
})

function handleClick(): void {
  if (!authStore.authenticated) {
    showAuthNotice()
    return
  }
  if (isSaved.value) {
    openOrganize()
    return
  }
  if (saveMutation.isPending.value) return
  errorMessage.value = null
  saveMutation.mutate()
}

function openOrganize(): void {
  toastOpen.value = false
  organizeError.value = ''
  newCollectionName.value = ''
  organizeOpen.value = true
}

function dismissToast(): void {
  clearTimeout(toastTimer)
  toastOpen.value = false
}

const addMutation = useAddCollectionPublication({
  mutation: {
    onSuccess: (_data, variables) => {
      addedCollectionIds.value.add(variables.id)
    },
    onError: (error) => {
      organizeError.value = normalizeHttpError(error).message
    },
  },
})

function addTo(collectionId: string): void {
  organizeError.value = ''
  addMutation.mutate({ id: collectionId, publicationId: props.publicationId })
}

const createMutation = useCreateCollection({
  mutation: {
    onSuccess: (collection) => {
      void queryClient.invalidateQueries({ queryKey: getGetUserCollectionsQueryKey(userId) })
      newCollectionName.value = ''
      addTo(collection.id)
    },
    onError: (error) => {
      organizeError.value = normalizeHttpError(error).message
    },
  },
})

function createAndAdd(): void {
  if (createMutation.isPending.value) return
  organizeError.value = ''
  if (!newCollectionName.value.trim()) {
    organizeError.value = 'Informe um nome para a coleção.'
    return
  }
  createMutation.mutate({ data: { name: newCollectionName.value.trim(), visibility: 'PRIVATE' } })
}

function removeFromSaved(): void {
  if (unsaveMutation.isPending.value) return
  organizeError.value = ''
  unsaveMutation.mutate()
}
</script>

<template>
  <div class="save-button">
    <button
      class="save-button__control"
      type="button"
      :aria-pressed="authStore.authenticated ? isSaved : undefined"
      :aria-label="
        !authStore.authenticated
          ? 'Salvar publicação; é necessário entrar'
          : isSaved
            ? 'Organizar nos salvos'
            : 'Salvar publicação'
      "
      @click="handleClick"
    >
      <AppIcon name="bookmark" :size="18" :stroke-width="1.8" />
      {{ isSaved ? 'Guardado' : 'Salvar' }}
    </button>
    <span v-if="errorMessage" class="save-button__error" role="alert">{{ errorMessage }}</span>

    <Teleport to="body">
      <div v-if="toastOpen" class="save-button__toast-wrapper">
        <div class="save-button__toast" role="status">
          <AppIcon name="check" :size="18" :stroke-width="2.2" />
          <span class="save-button__toast-text">Guardado nos seus salvos.</span>
          <button type="button" class="save-button__toast-action" @click="openOrganize">
            Escolher coleção
          </button>
          <button
            type="button"
            class="save-button__toast-close"
            aria-label="Dispensar"
            @click="dismissToast"
          >
            <AppIcon name="close" :size="16" :stroke-width="2.2" />
          </button>
        </div>
      </div>
    </Teleport>

    <BaseDialog v-model:open="organizeOpen" title="Guardar em" description="Organize nas suas coleções.">
      <div class="save-button__organize">
        <form class="save-button__create" @submit.prevent="createAndAdd">
          <BaseInput
            v-model="newCollectionName"
            label="Nova coleção"
            maxlength="80"
            hint="Ela começa como Só minha, você pode abrir depois."
          />
          <BaseButton type="submit" variant="secondary" :loading="createMutation.isPending.value">
            Criar e adicionar
          </BaseButton>
        </form>

        <BaseFieldError v-if="organizeError" :message="organizeError" />

        <div v-if="collectionsQuery.isPending.value" class="save-button__state">
          Carregando suas coleções...
        </div>
        <div
          v-else-if="!collectionsQuery.data.value?.content.length"
          class="save-button__state"
        >
          Você ainda não tem coleções.
        </div>
        <ul v-else class="save-button__list">
          <li v-for="collection in collectionsQuery.data.value.content" :key="collection.id">
            <span>{{ collection.name }}</span>
            <BaseButton
              variant="secondary"
              :disabled="addedCollectionIds.has(collection.id)"
              :loading="addMutation.isPending.value && addMutation.variables.value?.id === collection.id"
              @click="addTo(collection.id)"
            >
              {{ addedCollectionIds.has(collection.id) ? 'Adicionado' : 'Adicionar' }}
            </BaseButton>
          </li>
        </ul>
      </div>
      <template #actions>
        <BaseButton variant="danger" :loading="unsaveMutation.isPending.value" @click="removeFromSaved">
          Tirar dos salvos
        </BaseButton>
        <BaseButton @click="organizeOpen = false">Pronto</BaseButton>
      </template>
    </BaseDialog>
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
  gap: var(--space-2);
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

.save-button__error {
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}

.save-button__toast-wrapper {
  position: fixed;
  z-index: 90;
  right: var(--space-4);
  bottom: calc(var(--space-4) + env(safe-area-inset-bottom));
  left: var(--space-4);
  display: flex;
  justify-content: center;
}

.save-button__toast {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: min(100%, 26rem);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}

.save-button__toast svg {
  flex: none;
  color: var(--color-success);
}

.save-button__toast-text {
  flex: 1;
  font-size: var(--font-size-sm);
}

.save-button__toast-action {
  flex: none;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: none;
  border: 0;
}

.save-button__toast-close {
  display: grid;
  flex: none;
  width: 1.75rem;
  height: 1.75rem;
  color: var(--color-text-secondary);
  background: none;
  border: 0;
  border-radius: var(--radius-pill);
  place-items: center;
}

.save-button__organize {
  display: grid;
  gap: var(--space-4);
}

.save-button__create {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
}

.save-button__create :deep(.base-field) {
  flex: 1;
}

.save-button__state {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.save-button__list {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.save-button__list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
</style>
