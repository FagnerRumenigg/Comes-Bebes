<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import { report } from '@/api/generated/publications/publications'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{
  publicationId: string
  authorId: string
  reported: boolean
}>()

const emit = defineEmits<{ submitted: [] }>()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const open = ref(false)
const submitted = ref(props.reported)
const reasonCode = ref('NOT_FOOD')
const description = ref('')
const errorMessage = ref<string | null>(null)
const isOwnPublication = computed(() => authStore.identity?.userId === props.authorId)

watch(
  () => props.reported,
  (reported) => {
    submitted.value = reported
  },
)

const reasons = [
  { value: 'NOT_FOOD', label: 'Não é comida' },
  { value: 'IDENTIFIABLE_PERSON', label: 'Há uma pessoa identificável' },
  { value: 'UNAUTHORIZED_ADVERTISING', label: 'Publicidade não autorizada' },
  { value: 'AUTHORSHIP_VIOLATION', label: 'Violação de autoria' },
  { value: 'OFFENSIVE_OR_DISCRIMINATORY', label: 'Conteúdo ofensivo ou discriminatório' },
  { value: 'DANGEROUS_OR_ILLEGAL', label: 'Conteúdo perigoso ou ilegal' },
]

const mutation = useMutation({
  mutationFn: () =>
    report(props.publicationId, {
      reasonCode: reasonCode.value,
      description: description.value.trim() || undefined,
    }),
  onSuccess: () => {
    open.value = false
    submitted.value = true
    void queryClient.invalidateQueries({ queryKey: ['publications'] })
    emit('submitted')
  },
  onError: (error) => {
    errorMessage.value = normalizeHttpError(error).message
  },
})

function submit(): void {
  if (mutation.isPending.value) return
  errorMessage.value = null
  mutation.mutate()
}
</script>

<template>
  <button
    v-if="!authStore.authenticated"
    type="button"
    class="report-dialog__trigger"
    @click="showAuthNotice"
  >
    Denunciar
  </button>
  <template v-if="authStore.authenticated && !submitted && !isOwnPublication">
    <button type="button" class="report-dialog__trigger" @click="open = true">Denunciar</button>
    <BaseDialog
      v-model:open="open"
      title="Denunciar publicação"
      description="Escolha o motivo. A denúncia será analisada pela comunidade responsável pela moderação."
    >
      <form class="report-dialog" @submit.prevent="submit">
        <BaseSelect v-model="reasonCode" label="Motivo">
          <option v-for="reason in reasons" :key="reason.value" :value="reason.value">
            {{ reason.label }}
          </option>
        </BaseSelect>
        <BaseTextarea
          v-model="description"
          label="Descrição (opcional)"
          maxlength="1000"
          :rows="4"
        />
        <p v-if="errorMessage" class="report-dialog__error" role="alert">{{ errorMessage }}</p>
        <div class="report-dialog__actions">
          <BaseButton type="button" variant="ghost" @click="open = false">Cancelar</BaseButton>
          <BaseButton type="submit" :loading="mutation.isPending.value">Enviar denúncia</BaseButton>
        </div>
      </form>
    </BaseDialog>
  </template>
  <span v-else-if="submitted" class="report-dialog__reported">Denúncia enviada</span>
  <span v-else-if="authStore.authenticated && isOwnPublication" class="report-dialog__reported">
    Sua publicação
  </span>
</template>

<style scoped>
.report-dialog__trigger,
.report-dialog__reported {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.report-dialog__trigger {
  padding: var(--space-2);
  background: transparent;
  border: 0;
}

.report-dialog__trigger:hover {
  color: var(--color-danger);
}

.report-dialog {
  display: grid;
  gap: var(--space-4);
}

.report-dialog__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}

.report-dialog__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
