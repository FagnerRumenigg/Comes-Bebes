<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'

import {
  useAddCollectionPublication,
  useCreateCollection,
} from '@/api/generated/collections/collections'
import { getGetUserCollectionsQueryKey, useGetUserCollections } from '@/api/generated/users/users'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{ publicationId: string }>()

const authStore = useAuthStore()
const queryClient = useQueryClient()
const open = ref(false)
const newCollectionName = ref('')
const errorMessage = ref('')
const addedCollectionIds = ref(new Set<string>())

const userId = computed(() => authStore.identity?.userId ?? '')
const collectionsQuery = useGetUserCollections(userId, { page: 1, size: 50 })

function openDialog(): void {
  if (!authStore.authenticated) {
    showAuthNotice()
    return
  }
  errorMessage.value = ''
  newCollectionName.value = ''
  open.value = true
}

const addMutation = useAddCollectionPublication({
  mutation: {
    onSuccess: (_data, variables) => {
      addedCollectionIds.value.add(variables.id)
    },
    onError: (error) => {
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

function addTo(collectionId: string): void {
  errorMessage.value = ''
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
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

function createAndAdd(): void {
  if (createMutation.isPending.value) return
  errorMessage.value = ''
  if (!newCollectionName.value.trim()) {
    errorMessage.value = 'Informe um nome para a coleção.'
    return
  }
  createMutation.mutate({ data: { name: newCollectionName.value.trim(), visibility: 'PUBLIC' } })
}
</script>

<template>
  <div class="add-to-collection-button">
    <button type="button" class="add-to-collection-button__control" @click="openDialog">
      Adicionar a uma coleção
    </button>

    <BaseDialog
      v-model:open="open"
      title="Adicionar a uma coleção"
      description="Escolha uma coleção sua ou crie uma nova."
    >
      <div class="add-to-collection-button__content">
        <form class="add-to-collection-button__create" @submit.prevent="createAndAdd">
          <BaseInput v-model="newCollectionName" label="Nova coleção" maxlength="80" />
          <BaseButton
            type="submit"
            variant="secondary"
            :loading="createMutation.isPending.value"
          >
            Criar e adicionar
          </BaseButton>
        </form>

        <BaseFieldError v-if="errorMessage" :message="errorMessage" />

        <div v-if="collectionsQuery.isPending.value" class="add-to-collection-button__state">
          Carregando suas coleções...
        </div>
        <div
          v-else-if="!collectionsQuery.data.value?.content.length"
          class="add-to-collection-button__state"
        >
          Você ainda não tem coleções.
        </div>
        <ul v-else class="add-to-collection-button__list">
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
        <BaseButton variant="ghost" @click="open = false">Fechar</BaseButton>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.add-to-collection-button__control {
  padding: 0;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
}

.add-to-collection-button__content {
  display: grid;
  gap: var(--space-4);
}

.add-to-collection-button__create {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
}

.add-to-collection-button__create :deep(.base-field) {
  flex: 1;
}

.add-to-collection-button__state {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.add-to-collection-button__list {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.add-to-collection-button__list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
</style>
