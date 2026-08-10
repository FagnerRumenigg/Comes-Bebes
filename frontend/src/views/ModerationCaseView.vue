<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { useDecide, useGetModerationCaseById } from '@/api/generated/moderation/moderation'
import { useGetPublicationById } from '@/api/generated/publications/publications'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import { normalizeHttpError } from '@/api/errors'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const caseId = computed(() => String(route.params.caseId ?? ''))
const caseQuery = useGetModerationCaseById(caseId)
const publicationId = computed(() => caseQuery.data.value?.publicationId ?? '')
const publicationQuery = useGetPublicationById(publicationId)
const decision = ref<'KEPT' | 'HIDDEN' | 'REMOVED'>('KEPT')
const decisionNote = ref('')
const error = ref('')
const decideMutation = useDecide({
  mutation: {
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation', 'cases'] })
      queryClient.invalidateQueries({ queryKey: ['publications'] })
      void router.push('/admin/moderacao')
    },
    onError: (requestError) => {
      error.value = normalizeHttpError(requestError).message
    },
  },
})
const noteRequired = computed(() => decision.value === 'HIDDEN' || decision.value === 'REMOVED')

function submit(): void {
  if (decideMutation.isPending.value) return
  error.value = ''
  if (noteRequired.value && !decisionNote.value.trim()) {
    error.value = 'Justificativa obrigatória para ocultar ou remover.'
    return
  }
  decideMutation.mutate({
    id: caseId.value,
    data: {
      decision: decision.value,
      ...(decisionNote.value.trim() ? { decisionNote: decisionNote.value.trim() } : {}),
    },
  })
}
</script>

<template>
  <section class="moderation-case">
    <RouterLink class="moderation-case__back" to="/admin/moderacao"
      >← Voltar para a fila</RouterLink
    >
    <div v-if="caseQuery.isPending.value" class="moderation-case__state">Carregando caso...</div>
    <div v-else-if="caseQuery.isError.value" class="moderation-case__state" role="alert">
      Caso não encontrado.
    </div>
    <template v-else-if="caseQuery.data.value"
      ><header class="moderation-case__header">
        <p class="moderation-case__eyebrow">Caso de moderação</p>
        <h1>Análise da publicação</h1>
        <p>{{ caseQuery.data.value.reportCountAtOpen }} denúncias registradas.</p>
      </header>
      <article v-if="publicationQuery.data.value" class="moderation-case__publication">
        <h2>{{ publicationQuery.data.value.title ?? 'Publicação sem título' }}</h2>
        <p>{{ publicationQuery.data.value.description ?? 'Sem descrição.' }}</p>
        <RouterLink :to="`/publicacoes/${publicationQuery.data.value.id}`"
          >Ver publicação completa</RouterLink
        >
      </article>
      <form class="moderation-case__form" novalidate @submit.prevent="submit">
        <BaseSelect v-model="decision" label="Decisão" required
          ><option value="KEPT">Manter publicação</option>
          <option value="HIDDEN">Ocultar publicação</option>
          <option value="REMOVED">Remover publicação</option></BaseSelect
        ><BaseTextarea
          v-model="decisionNote"
          label="Justificativa"
          hint="Obrigatória para ocultar ou remover."
          maxlength="2000"
          :required="noteRequired"
          :rows="5"
        /><BaseFieldError v-if="error" :message="error" /><BaseButton
          type="submit"
          :loading="decideMutation.isPending.value"
          >Registrar decisão</BaseButton
        >
      </form></template
    >
  </section>
</template>

<style scoped>
.moderation-case {
  max-width: 50rem;
  margin-inline: auto;
}
.moderation-case__back {
  display: inline-block;
  margin-block-end: var(--space-6);
  color: var(--color-primary);
}
.moderation-case__header {
  margin-block-end: var(--space-8);
}
.moderation-case__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.moderation-case h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.moderation-case__header p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.moderation-case__publication,
.moderation-case__form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.moderation-case__publication {
  margin-block-end: var(--space-6);
}
.moderation-case__publication h2 {
  margin: 0;
  font-family: var(--font-family-display);
}
.moderation-case__publication p {
  color: var(--color-text-secondary);
}
.moderation-case__publication a {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}
.moderation-case__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
