<script setup lang="ts">
import { ref, watch } from 'vue'

import { useCreateCollection, useUpdateCollection } from '@/api/generated/collections/collections'
import type { CollectionResponse } from '@/api/generated/models'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'

const props = defineProps<{
  open: boolean
  collection?: CollectionResponse
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [collection: CollectionResponse]
}>()

const name = ref('')
const description = ref('')
const visibility = ref<'PUBLIC' | 'PRIVATE'>('PUBLIC')
const errorMessage = ref('')

function resetForm(): void {
  name.value = props.collection?.name ?? ''
  description.value = props.collection?.description ?? ''
  visibility.value = props.collection?.visibility ?? 'PUBLIC'
  errorMessage.value = ''
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) resetForm()
  },
)

const createMutation = useCreateCollection({
  mutation: {
    onSuccess: (collection) => {
      emit('saved', collection)
      emit('update:open', false)
    },
    onError: (error) => {
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

const updateMutation = useUpdateCollection({
  mutation: {
    onSuccess: (collection) => {
      emit('saved', collection)
      emit('update:open', false)
    },
    onError: (error) => {
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

const isPending = () => createMutation.isPending.value || updateMutation.isPending.value

function submit(): void {
  if (isPending()) return
  errorMessage.value = ''
  if (!name.value.trim()) {
    errorMessage.value = 'Informe um nome para a coleção.'
    return
  }
  const data = {
    name: name.value.trim(),
    description: description.value.trim() || undefined,
    visibility: visibility.value,
  }
  if (props.collection) {
    updateMutation.mutate({ id: props.collection.id, data })
  } else {
    createMutation.mutate({ data })
  }
}
</script>

<template>
  <BaseDialog
    :open="open"
    :title="collection ? 'Editar coleção' : 'Nova coleção'"
    description="Organize publicações em uma lista curada, pública ou só sua."
    @update:open="(value) => emit('update:open', value)"
  >
    <form class="collection-form-dialog" @submit.prevent="submit">
      <BaseInput v-model="name" label="Nome" required maxlength="80" />
      <BaseTextarea v-model="description" label="Descrição (opcional)" maxlength="280" :rows="3" />
      <BaseSelect v-model="visibility" label="Visibilidade">
        <option value="PUBLIC">Pública</option>
        <option value="PRIVATE">Privada</option>
      </BaseSelect>
      <BaseFieldError v-if="errorMessage" :message="errorMessage" />
    </form>
    <template #actions>
      <BaseButton variant="ghost" @click="emit('update:open', false)">Cancelar</BaseButton>
      <BaseButton :loading="isPending()" @click="submit">
        {{ collection ? 'Salvar' : 'Criar coleção' }}
      </BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.collection-form-dialog {
  display: grid;
  gap: var(--space-4);
}
</style>
