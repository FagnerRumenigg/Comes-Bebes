<script setup lang="ts">
import { useListFeedback } from '@/api/generated/feedback/feedback'
import { formatNotificationWhen } from '@/features/notifications/notifications'

const feedbackQuery = useListFeedback({ page: 1, size: 50 })
</script>

<template>
  <section class="feedback-queue-view">
    <header class="feedback-queue-view__header">
      <p class="feedback-queue-view__eyebrow">Administração</p>
      <h1>Falar com a gente</h1>
      <p>Mensagens enviadas pelas pessoas que usam o Comes&amp;Bebes, mais recente primeiro.</p>
    </header>

    <div v-if="feedbackQuery.isPending.value" class="feedback-queue-view__state">
      Carregando mensagens...
    </div>
    <div v-else-if="feedbackQuery.isError.value" class="feedback-queue-view__state" role="alert">
      Não foi possível carregar as mensagens.
    </div>
    <div v-else-if="!feedbackQuery.data.value?.content.length" class="feedback-queue-view__state">
      Nenhuma mensagem enviada ainda.
    </div>

    <ul v-else class="feedback-queue-view__list">
      <li v-for="item in feedbackQuery.data.value.content" :key="item.id">
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
