<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { httpClient } from '@/api/client'

const POLL_INTERVAL_MS = 5_000
const POLL_TIMEOUT_MS = 4_000
const ELAPSED_TICK_MS = 1_000

interface Stage {
  at: number
  msg: string
  sub: string
  retry?: boolean
  cancel?: boolean
  fail?: boolean
}

// Cada estágio só troca o texto — nada de barra de progresso falsa
// (docs/telas/03-carregando.html). O backend real pode acordar de um
// scale-to-zero do Azure Container Apps em até ~2 minutos.
const STAGES: Stage[] = [
  {
    at: 0,
    msg: 'Preparando tudo para você',
    sub: 'Estamos ligando nossos servidores. Leva alguns segundos.',
  },
  {
    at: 10,
    msg: 'Ainda estamos aquecendo',
    sub: 'Só mais um pouquinho. Pode deixar a tela aberta.',
  },
  {
    at: 25,
    msg: 'Está demorando mais que o normal',
    sub: 'Você pode esperar mais um pouco ou tentar de novo.',
    retry: true,
  },
  {
    at: 90,
    msg: 'Está demorando bem mais que o normal',
    sub: 'Pode ser a nossa conexão ou a sua. Continue tentando de novo em instantes.',
    retry: true,
    fail: true,
  },
]

const checking = ref(false)
const elapsedSeconds = ref(0)
let pollHandle: ReturnType<typeof setInterval> | undefined
let elapsedHandle: ReturnType<typeof setInterval> | undefined
const startedAt = Date.now()

const stage = computed(() => {
  let current = STAGES[0]
  for (const candidate of STAGES) {
    if (elapsedSeconds.value >= candidate.at) current = candidate
  }
  return current
})

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
  elapsedHandle = setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000)
  }, ELAPSED_TICK_MS)
})

onUnmounted(() => {
  if (pollHandle) clearInterval(pollHandle)
  if (elapsedHandle) clearInterval(elapsedHandle)
})
</script>

<template>
  <section class="backend-offline" aria-labelledby="backend-offline-title">
    <div class="backend-offline__card" :class="{ 'backend-offline__card--fail': stage.fail }">
      <svg
        class="backend-offline__pot"
        :class="{ 'backend-offline__pot--running': !stage.fail }"
        viewBox="0 0 140 200"
        role="img"
        aria-label="Cafeteira preparando café"
      >
        <path
          class="backend-offline__steam backend-offline__steam--1"
          d="M56 74 C50 62 62 56 56 44 C51 34 60 28 56 18"
        />
        <path
          class="backend-offline__steam backend-offline__steam--2"
          d="M70 70 C64 57 76 51 70 38 C65 27 74 21 70 10"
        />
        <path
          class="backend-offline__steam backend-offline__steam--3"
          d="M84 74 C78 62 90 56 84 44 C79 34 88 28 84 18"
        />
        <g class="backend-offline__pot-shape">
          <path class="backend-offline__body" d="M47 103 L28 113 L33 126 L50 119 Z" />
          <path class="backend-offline__body" d="M53 97 L87 97 L94 143 L46 143 Z" />
          <rect class="backend-offline__body" x="51" y="86" width="38" height="12" rx="5" />
          <circle class="backend-offline__knob" cx="70" cy="80" r="7" />
          <path
            class="backend-offline__body"
            d="M94 110 C117 110 121 132 101 144 L95 137 C110 128 108 118 94 118 Z"
          />
          <rect class="backend-offline__band" x="44" y="141" width="52" height="7" rx="3" />
          <path class="backend-offline__body" d="M46 147 L94 147 L101 188 L39 188 Z" />
          <path class="backend-offline__shine" d="M58 100 L64 100 L57 141 L51 141 Z" />
          <path class="backend-offline__shine" d="M51 151 L57 151 L51 185 L45 185 Z" />
        </g>
      </svg>

      <h1 id="backend-offline-title">{{ stage.msg }}</h1>
      <p aria-live="polite">{{ stage.sub }}</p>
      <p class="backend-offline__elapsed">{{ elapsedSeconds }}s</p>

      <div v-if="stage.retry" class="backend-offline__actions">
        <button type="button" class="backend-offline__retry" @click="checkNow">
          Tentar de novo
        </button>
      </div>
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
  width: min(100%, 27.5rem);
  padding: var(--space-10) var(--space-8) var(--space-8);
  text-align: center;
  background: var(--color-background);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.backend-offline__pot {
  display: block;
  width: 8.25rem;
  height: 11.875rem;
  margin: 0 auto 0.375rem;
}

.backend-offline__body {
  fill: var(--color-primary);
}

.backend-offline__band,
.backend-offline__knob {
  fill: var(--color-accent);
}

.backend-offline__shine {
  fill: var(--color-primary-contrast);
  opacity: 0.16;
}

.backend-offline__steam {
  fill: none;
  stroke: var(--color-text-secondary);
  stroke-linecap: round;
  stroke-width: 5;
  opacity: 0;
}

.backend-offline__pot--running .backend-offline__steam {
  animation: backend-offline-rise 2.9s ease-out infinite;
  transform-origin: 50% 100%;
}

.backend-offline__pot--running .backend-offline__steam--2 {
  animation-delay: 0.45s;
}

.backend-offline__pot--running .backend-offline__steam--3 {
  animation-delay: 0.95s;
}

.backend-offline__pot--running .backend-offline__pot-shape {
  animation: backend-offline-warm 2.9s ease-in-out infinite;
  transform-origin: 50% 100%;
}

@keyframes backend-offline-rise {
  0% {
    opacity: 0;
    transform: translateY(0.75rem) scaleY(0.8);
  }

  22% {
    opacity: 0.75;
  }

  60% {
    opacity: 0.45;
  }

  100% {
    opacity: 0;
    transform: translateY(-1rem) scaleY(1.15);
  }
}

@keyframes backend-offline-warm {
  0%,
  100% {
    transform: scaleY(1);
  }

  50% {
    transform: scaleY(1.015);
  }
}

@media (prefers-reduced-motion: reduce) {
  .backend-offline__pot--running .backend-offline__steam,
  .backend-offline__pot--running .backend-offline__pot-shape {
    animation: none;
  }

  .backend-offline__pot--running .backend-offline__steam {
    opacity: 0.5;
  }
}

.backend-offline__card--fail .backend-offline__body,
.backend-offline__card--fail .backend-offline__band,
.backend-offline__card--fail .backend-offline__knob {
  fill: var(--color-border);
}

.backend-offline__card--fail h1 {
  color: var(--color-danger);
}

h1 {
  margin-bottom: var(--space-2);
  font-size: var(--font-size-2xl);
}

p {
  color: var(--color-text-secondary);
  line-height: var(--line-height-body);
}

.backend-offline__elapsed {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-variant-numeric: tabular-nums;
  opacity: 0.7;
}

.backend-offline__actions {
  margin-top: var(--space-6);
}

.backend-offline__retry {
  display: flex;
  width: 100%;
  min-height: 3rem;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-contrast);
  font: inherit;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.backend-offline__retry:hover {
  filter: brightness(1.08);
}
</style>
