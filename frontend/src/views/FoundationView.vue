<script setup lang="ts">
import { useQueryClient } from '@tanstack/vue-query'
import { getActivePinia } from 'pinia'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const queryClientReady = Boolean(useQueryClient())
const piniaReady = Boolean(getActivePinia())
const route = useRoute()
const title = computed(() => route.meta.pageTitle ?? 'Fundação técnica pronta')
</script>

<template>
  <article class="foundation-view">
    <p class="foundation-view__eyebrow">Estrutura de navegação pronta</p>
    <h1>{{ title }}</h1>
    <p>O conteúdo definitivo desta rota será implementado na fase funcional correspondente.</p>
    <dl class="foundation-view__status">
      <div>
        <dt>Pinia</dt>
        <dd>{{ piniaReady ? 'ativo' : 'indisponível' }}</dd>
      </div>
      <div>
        <dt>Vue Query</dt>
        <dd>{{ queryClientReady ? 'ativo' : 'indisponível' }}</dd>
      </div>
    </dl>
  </article>
</template>

<style scoped>
.foundation-view {
  max-width: var(--content-feed);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.foundation-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.foundation-view h1 + p {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}

.foundation-view__status {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-6);
  margin-block: var(--space-6) 0;
}

.foundation-view__status div {
  display: grid;
  gap: var(--space-1);
}

.foundation-view__status dt {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
}

.foundation-view__status dd {
  margin: 0;
  font-weight: var(--font-weight-semibold);
}
</style>
