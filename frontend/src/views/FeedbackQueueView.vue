<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { apiRequest } from '@/api/client'
import { useListFeedback } from '@/api/generated/feedback/feedback'
import { formatNotificationWhen } from '@/features/notifications/notifications'

const feedbackQuery = useListFeedback({ page: 1, size: 50 })
const queryClient = useQueryClient()
const categoryFilter = ref('ALL')
const statusFilter = ref('ALL')
const CATEGORY_LABELS = {
  SUGGESTION: 'Sugestão',
  BUG: 'Problema',
  QUESTION: 'Dúvida',
  CONTACT: 'Contato',
} as const
const STATUS_LABELS = {
  NEW: 'Novo',
  IN_REVIEW: 'Em análise',
  RESPONDED: 'Respondido',
  ARCHIVED: 'Arquivado',
} as const
const filteredFeedback = computed(() =>
  (feedbackQuery.data.value?.content ?? []).filter(
    (item) =>
      (categoryFilter.value === 'ALL' || (item.category ?? 'SUGGESTION') === categoryFilter.value) &&
      (statusFilter.value === 'ALL' || (item.status ?? 'NEW') === statusFilter.value),
  ),
)
const triageMutation = useMutation({
  mutationFn: ({ id, category, status }: { id: string; category: string; status: string }) =>
    apiRequest<void>({
      url: `/feedback/${id}`,
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      data: { category, status },
    }),
  onSuccess: () => {
    void queryClient.invalidateQueries({ queryKey: ['feedback'] })
  },
})

function updateTriage(item: (typeof filteredFeedback.value)[number], field: 'category' | 'status', event: Event): void {
  const value = (event.target as HTMLSelectElement).value
  triageMutation.mutate({
    id: item.id,
    category: field === 'category' ? value : item.category ?? 'SUGGESTION',
    status: field === 'status' ? value : item.status ?? 'NEW',
  })
}
</script>

<template>
  <section class="feedback-queue-view">
    <header class="feedback-queue-view__header">
      <p class="feedback-queue-view__eyebrow">Administração</p>
      <h1>Falar com a gente</h1>
      <p>Mensagens enviadas pelas pessoas que usam o Comes&amp;Bebes, mais recente primeiro.</p>
    </header>

    <div class="feedback-queue-view__filters" aria-label="Filtrar feedbacks">
      <label>Tipo
        <select v-model="categoryFilter">
          <option value="ALL">Todos</option>
          <option v-for="(label, value) in CATEGORY_LABELS" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
      <label>Status
        <select v-model="statusFilter">
          <option value="ALL">Todos</option>
          <option v-for="(label, value) in STATUS_LABELS" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
    </div>

    <div v-if="feedbackQuery.isPending.value" class="feedback-queue-view__state">
      Carregando mensagens...
    </div>
    <div v-else-if="feedbackQuery.isError.value" class="feedback-queue-view__state" role="alert">
      Não foi possível carregar as mensagens.
    </div>
    <div v-else-if="!filteredFeedback.length" class="feedback-queue-view__state">
      Nenhuma mensagem enviada ainda.
    </div>

    <ul v-else class="feedback-queue-view__list">
      <li v-for="item in filteredFeedback" :key="item.id">
        <div class="feedback-queue-view__labels">
          <span class="feedback-queue-view__badge">{{ CATEGORY_LABELS[item.category ?? 'SUGGESTION'] }}</span>
          <span class="feedback-queue-view__badge feedback-queue-view__badge--status">{{ STATUS_LABELS[item.status ?? 'NEW'] }}</span>
        </div>
        <div class="feedback-queue-view__triage">
          <label>Tipo
            <select :value="item.category ?? 'SUGGESTION'" :disabled="triageMutation.isPending.value" @change="updateTriage(item, 'category', $event)">
              <option v-for="(label, value) in CATEGORY_LABELS" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label>Status
            <select :value="item.status ?? 'NEW'" :disabled="triageMutation.isPending.value" @change="updateTriage(item, 'status', $event)">
              <option v-for="(label, value) in STATUS_LABELS" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
        </div>
        <p class="feedback-queue-view__message">{{ item.message }}</p>
        <div class="feedback-queue-view__meta">
          <span>
            {{ item.userDisplayName ?? 'Alguém' }}<template v-if="item.username">
              · @{{ item.username }}</template
            >
          </span>
          <span v-if="item.contactEmail">Responder em: {{ item.contactEmail }}</span>
          <span>{{ formatNotificationWhen(item.createdAt) }}</span>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.feedback-queue-view {
  max-width: 50rem;
  margin-inline: auto;
}

.feedback-queue-view__header {
  margin-block-end: var(--space-8);
}

.feedback-queue-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.feedback-queue-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}

.feedback-queue-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}

.feedback-queue-view__list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  list-style: none;
}

.feedback-queue-view__filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-block-end: var(--space-5);
}

.feedback-queue-view__filters label {
  display: grid;
  gap: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.feedback-queue-view__filters select {
  min-width: 10rem;
  min-height: 2.5rem;
  padding-inline: var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.feedback-queue-view__labels {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.feedback-queue-view__triage {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.feedback-queue-view__triage label {
  display: grid;
  gap: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.feedback-queue-view__triage select {
  min-height: 2.25rem;
  padding-inline: var(--space-2);
  color: var(--color-text);
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.feedback-queue-view__badge {
  padding: var(--space-1) var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  border-radius: var(--radius-pill);
}

.feedback-queue-view__badge--status {
  color: var(--color-text-secondary);
  background: var(--color-surface-raised);
}

.feedback-queue-view__list li {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.feedback-queue-view__message {
  margin: 0;
  white-space: pre-wrap;
}

.feedback-queue-view__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1) var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.feedback-queue-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
