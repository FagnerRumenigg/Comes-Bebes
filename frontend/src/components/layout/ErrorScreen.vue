<script setup lang="ts">
import { computed } from 'vue'

import BaseButton from '@/components/base/BaseButton.vue'

import SheetPage from './SheetPage.vue'

export type ErrorCause = 'offline' | 'server' | 'unknown'

const props = defineProps<{
  cause: ErrorCause
}>()

const emit = defineEmits<{
  retry: []
  home: []
}>()

const CONTENT: Record<ErrorCause, { title: string; text: string; home: boolean }> = {
  offline: {
    title: 'Você está sem internet',
    text: 'Parece que a conexão caiu. Assim que ela voltar, é só tentar de novo.',
    home: false,
  },
  server: {
    title: 'A cozinha deu uma engasgada',
    text: 'O problema é nosso, não seu. Tente de novo em alguns instantes.',
    home: true,
  },
  unknown: {
    title: 'Alguma coisa entornou aqui',
    text: 'Aconteceu um erro inesperado. Tentar de novo costuma resolver.',
    home: true,
  },
}

const content = computed(() => CONTENT[props.cause])
</script>

<template>
  <SheetPage>
    <section class="error-screen" aria-labelledby="error-screen-title">
      <svg class="error-screen__art" viewBox="0 0 132 132" role="img" aria-label="Panela tombada">
        <g transform="rotate(-28 66 76)">
          <path class="error-screen__pot" d="M30 62h60v22a12 12 0 0 1-12 12H42a12 12 0 0 1-12-12z" />
          <rect class="error-screen__pot" x="24" y="54" width="72" height="8" rx="4" />
          <rect class="error-screen__pot" x="57" y="46" width="6" height="9" rx="3" />
          <path class="error-screen__pot" d="M90 66h10a8 8 0 0 1 0 16H90z" />
        </g>
        <path
          class="error-screen__spill"
          d="M34 108c10-7 24-8 36-5 9 2 18 2 26-2 5-2 8 5 3 8-11 6-24 6-35 4-8-2-16-1-23 3-5 3-11-4-7-8z"
        />
        <circle class="error-screen__drop error-screen__drop--1" cx="88" cy="92" r="3.2" />
        <circle class="error-screen__drop error-screen__drop--2" cx="46" cy="96" r="2.6" />
      </svg>

      <h1 id="error-screen-title">{{ content.title }}</h1>
      <p>{{ content.text }}</p>

      <div class="error-screen__actions">
        <BaseButton variant="primary" @click="emit('retry')">Tentar de novo</BaseButton>
        <BaseButton v-if="content.home" variant="ghost" @click="emit('home')">
          Voltar ao início
        </BaseButton>
      </div>
      <p class="error-screen__tiny">Nada do que você escreveu foi perdido.</p>
    </section>
  </SheetPage>
</template>

<style scoped>
.error-screen {
  text-align: center;
}

.error-screen__art {
  display: block;
  width: 8.25rem;
  height: 8.25rem;
  margin: 0 auto var(--space-5);
}

.error-screen__pot {
  fill: var(--color-border);
}

.error-screen__spill {
  fill: var(--color-accent);
  opacity: 0.85;
}

.error-screen__drop {
  fill: var(--color-accent);
  opacity: 0.7;
  animation: error-screen-drip 2.6s ease-in infinite;
}

.error-screen__drop--2 {
  animation-delay: 1.2s;
}

@media (prefers-reduced-motion: reduce) {
  .error-screen__drop {
    animation: none;
    opacity: 0.6;
  }
}

@keyframes error-screen-drip {
  0% {
    transform: translateY(0);
    opacity: 0;
  }

  25% {
    opacity: 0.7;
  }

  100% {
    transform: translateY(0.8125rem);
    opacity: 0;
  }
}

.error-screen h1 {
  margin-bottom: var(--space-3);
  font-family: var(--font-editorial);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-regular);
}

.error-screen p {
  max-width: 27em;
  margin: 0 auto var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
}

.error-screen__actions {
  display: grid;
  max-width: 20rem;
  gap: var(--space-2);
  margin: 0 auto;
}

.error-screen__tiny {
  margin-top: var(--space-4);
  font-size: var(--font-size-sm);
  opacity: 0.85;
}
</style>
