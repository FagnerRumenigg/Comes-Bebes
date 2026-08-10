<script setup lang="ts">
import { usePending } from '@/api/generated/moderation/moderation'

const pendingQuery = usePending()
</script>

<template>
  <section class="moderation-view">
    <header class="moderation-view__header">
      <p class="moderation-view__eyebrow">Administração</p>
      <h1>Fila de moderação</h1>
      <p>Analise denúncias pendentes e mantenha a comunidade segura.</p>
    </header>
    <div v-if="pendingQuery.isPending.value" class="moderation-view__state">
      Carregando casos...
    </div>
    <div v-else-if="pendingQuery.isError.value" class="moderation-view__state" role="alert">
      Não foi possível carregar a fila.
    </div>
    <div v-else-if="!pendingQuery.data.value?.length" class="moderation-view__state">
      Não há casos pendentes.
    </div>
    <ul v-else class="moderation-view__list">
      <li v-for="item in pendingQuery.data.value" :key="item.id">
        <div>
          <strong>Caso {{ item.id }}</strong
          ><span
            >{{ item.reportCountAtOpen }} denúncias · aberto em
            {{ new Date(item.openedAt).toLocaleDateString('pt-BR') }}</span
          >
        </div>
        <RouterLink :to="`/admin/moderacao/${item.id}`">Analisar caso</RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.moderation-view {
  max-width: 50rem;
  margin-inline: auto;
}
.moderation-view__header {
  margin-block-end: var(--space-8);
}
.moderation-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.moderation-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.moderation-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.moderation-view__list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  list-style: none;
}
.moderation-view__list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.moderation-view__list li > div {
  display: grid;
  gap: var(--space-2);
}
.moderation-view__list span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.moderation-view__list a {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}
.moderation-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
