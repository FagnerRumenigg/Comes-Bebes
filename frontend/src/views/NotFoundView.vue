<script setup lang="ts">
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const authStore = useAuthStore()

function goBack(): void {
  if (window.history.state?.back) {
    router.back()
    return
  }
  void router.push('/')
}
</script>

<template>
  <section class="not-found-view" aria-labelledby="not-found-title">
    <p class="not-found-view__eyebrow">Erro 404</p>
    <h1 id="not-found-title">Não encontramos essa página.</h1>
    <p>O endereço pode ter mudado ou o conteúdo não está mais disponível.</p>
    <nav class="not-found-view__actions" aria-label="Caminhos para continuar">
      <button type="button" class="not-found-view__link not-found-view__link--secondary" @click="goBack">
        Voltar
      </button>
      <RouterLink class="not-found-view__link" to="/">Ir para o feed</RouterLink>
      <RouterLink v-if="authStore.authenticated" class="not-found-view__link not-found-view__link--secondary" to="/salvos">
        Abrir salvos
      </RouterLink>
      <RouterLink v-if="authStore.authenticated" class="not-found-view__link not-found-view__link--secondary" to="/publicar">
        Publicar algo
      </RouterLink>
    </nav>
  </section>
</template>

<style scoped>
.not-found-view {
  display: grid;
  max-width: var(--content-narrow);
  justify-items: start;
  gap: var(--space-4);
  margin-inline: auto;
  padding-block: var(--space-16);
}

.not-found-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.not-found-view__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.not-found-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: var(--font-size-3xl);
}

.not-found-view > p:not(.not-found-view__eyebrow) {
  margin: 0;
  color: var(--color-text-secondary);
}

.not-found-view__link {
  display: inline-flex;
  min-height: var(--control-min-size);
  align-items: center;
  padding-inline: var(--space-5);
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

.not-found-view__link:hover {
  filter: brightness(1.08);
}

.not-found-view__link--secondary {
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: none;
}
</style>
