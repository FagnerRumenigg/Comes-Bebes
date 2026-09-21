<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { useDecide, useGetModerationCaseById } from '@/api/generated/moderation/moderation'
import { useGetPublicationById } from '@/api/generated/publications/publications'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
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
const confirmationOpen = ref(false)
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
const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

function submit(): void {
  if (decideMutation.isPending.value) return
  error.value = ''
  if (noteRequired.value && !decisionNote.value.trim()) {
    error.value = 'Justificativa obrigatória para ocultar ou remover.'
    return
  }
  if (noteRequired.value) {
    confirmationOpen.value = true
    return
  }
  sendDecision()
}

function sendDecision(): void {
  if (decideMutation.isPending.value) return
  confirmationOpen.value = false
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
      <aside class="moderation-case__evidence">
        <h2>Registro do caso</h2>
        <dl>
          <div><dt>Aberto em</dt><dd>{{ formatDate(caseQuery.data.value.openedAt) }}</dd></div>
          <div><dt>Denúncias na abertura</dt><dd>{{ caseQuery.data.value.reportCountAtOpen }}</dd></div>
          <div><dt>Status</dt><dd>{{ caseQuery.data.value.status }}</dd></div>
        </dl>
        <p>As denúncias associadas e a publicação completa devem ser consultadas antes da decisão.</p>
        <ul v-if="caseQuery.data.value.reports?.length" class="moderation-case__reports">
          <li v-for="report in caseQuery.data.value.reports" :key="report.id">
            <strong>Motivo {{ report.reasonId }}</strong>
            <span>{{ report.description || 'Sem descrição adicional.' }}</span>
            <small>{{ formatDate(report.createdAt) }} · {{ report.resolution }}</small>
          </li>
        </ul>
        <p v-else>Não há detalhes de denúncias disponíveis para este caso.</p>
      </aside>
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
    <BaseDialog
      v-model:open="confirmationOpen"
      :title="decision === 'REMOVED' ? 'Remover publicação?' : 'Ocultar publicação?'"
      :description="decision === 'REMOVED' ? 'A publicação será removida e essa decisão ficará registrada no caso.' : 'A publicação deixará de aparecer enquanto o caso permanecer resolvido.'"
    >
      <template #actions>
        <BaseButton variant="ghost" @click="confirmationOpen = false">Voltar</BaseButton>
        <BaseButton variant="danger" :loading="decideMutation.isPending.value" @click="sendDecision">
          Confirmar decisão
        </BaseButton>
      </template>
    </BaseDialog>
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
.moderation-case__evidence {
  margin-block-end: var(--space-6);
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.moderation-case__evidence h2 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-family-display);
}
.moderation-case__evidence dl {
  display: grid;
  gap: var(--space-3);
  margin: 0;
}
.moderation-case__evidence dl div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
}
.moderation-case__evidence dt { color: var(--color-text-secondary); }
.moderation-case__evidence dd { margin: 0; font-weight: var(--font-weight-semibold); }
.moderation-case__evidence p { margin: var(--space-4) 0 0; color: var(--color-text-secondary); }
.moderation-case__reports { display: grid; gap: var(--space-3); padding: 0; margin: var(--space-4) 0 0; list-style: none; }
.moderation-case__reports li { display: grid; gap: var(--space-1); padding-block-start: var(--space-3); border-block-start: 1px solid var(--color-border); }
.moderation-case__reports span, .moderation-case__reports small { color: var(--color-text-secondary); }
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
