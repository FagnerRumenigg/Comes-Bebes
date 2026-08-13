<script setup lang="ts">
import BaseButton from '@/components/base/BaseButton.vue'

function retry(): void {
  window.location.reload()
}
</script>

<template>
  <section class="backend-offline" aria-labelledby="backend-offline-title">
    <div class="backend-offline__card">
      <p class="backend-offline__eyebrow">Fase de testes</p>
      <h1 id="backend-offline-title">😴 O Comes&amp;Bebes está dormindo</h1>
      <p>
        Não conseguimos falar com o servidor agora. Se for entre <strong>23h e 8h</strong>, é o
        horário de sono normal desta fase de testes; fora desse horário, pode ser só um problema
        passageiro.
      </p>
      <p>Tenta de novo daqui a pouco.</p>
      <BaseButton class="backend-offline__retry" @click="retry">Tentar novamente</BaseButton>
    </div>
  </section>
</template>

<style scoped>
.backend-offline {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: var(--space-8);
  background: var(--color-background);
}

.backend-offline__card {
  width: min(100%, 32rem);
  display: grid;
  gap: var(--space-4);
  padding: var(--space-8);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.backend-offline__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.backend-offline h1 {
  margin: 0;
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
}

.backend-offline p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.backend-offline__retry {
  justify-self: center;
}
</style>
