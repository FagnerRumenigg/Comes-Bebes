<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

import { httpClient } from '@/api/client'
import BaseButton from '@/components/base/BaseButton.vue'

const POLL_INTERVAL_MS = 5_000
const POLL_TIMEOUT_MS = 4_000

const checking = ref(false)
let pollHandle: ReturnType<typeof setInterval> | undefined

async function checkNow(): Promise<void> {
  if (checking.value) return
  checking.value = true
  try {
    // Sucesso aqui já marca o backend como online via interceptor do
    // httpClient (markBackendOnline) - a tela some sozinha, sem reload.
    await httpClient.get('/actuator/health/liveness', { timeout: POLL_TIMEOUT_MS })
  } catch {
    // Ainda fora do ar - só segue tentando no próximo intervalo.
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  pollHandle = setInterval(checkNow, POLL_INTERVAL_MS)
})

onUnmounted(() => {
  if (pollHandle) clearInterval(pollHandle)
})
</script>

<template>
  <section class="backend-offline" aria-labelledby="backend-offline-title">
    <div class="backend-offline__card">
      <p class="backend-offline__eyebrow">Fase de testes</p>
      <h1 id="backend-offline-title">😴 O Comes&amp;Bebes está dormindo</h1>
      <p>
        Pra economizar enquanto estamos testando, o servidor desliga sozinho depois de alguns
        minutos sem uso e acorda de novo assim que alguém tenta acessar. Isso costuma levar
        <strong>até 2 minutos</strong> na primeira tentativa.
      </p>
      <p aria-live="polite">
        {{ checking ? 'Verificando…' : 'Verificando automaticamente a cada poucos segundos.' }}
        Essa tela some sozinha assim que o servidor acordar.
      </p>
      <BaseButton class="backend-offline__retry" :disabled="checking" @click="checkNow">
        Verificar agora
      </BaseButton>
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
