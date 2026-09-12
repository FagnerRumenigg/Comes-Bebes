<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const CURIOSITIES = [
  'O calor libera aromas que a água fria não consegue extrair.',
  'Uma pitada de sal pode realçar o sabor doce de uma receita.',
  'A crocância também faz parte do sabor que a gente percebe.',
  'Descansar uma massa ajuda a hidratar melhor a farinha.',
]

const curiosityIndex = ref(0)
let curiosityTimer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  curiosityTimer = setInterval(() => {
    curiosityIndex.value = (curiosityIndex.value + 1) % CURIOSITIES.length
  }, 6_000)
})

onBeforeUnmount(() => {
  if (curiosityTimer) clearInterval(curiosityTimer)
})
</script>

<template>
  <div class="publication-processing" role="dialog" aria-modal="true" aria-labelledby="publication-processing-title">
    <div class="publication-processing__card">
      <svg class="publication-processing__pot" viewBox="0 0 140 200" role="img" aria-label="Bule preparando a publicação">
        <path class="publication-processing__steam publication-processing__steam--1" d="M56 74 C50 62 62 56 56 44 C51 34 60 28 56 18" />
        <path class="publication-processing__steam publication-processing__steam--2" d="M70 70 C64 57 76 51 70 38 C65 27 74 21 70 10" />
        <path class="publication-processing__steam publication-processing__steam--3" d="M84 74 C78 62 90 56 84 44 C79 34 88 28 84 18" />
        <g class="publication-processing__pot-shape">
          <path class="publication-processing__body" d="M47 103 L28 113 L33 126 L50 119 Z" />
          <path class="publication-processing__body" d="M53 97 L87 97 L94 143 L46 143 Z" />
          <rect class="publication-processing__body" x="51" y="86" width="38" height="12" rx="5" />
          <circle class="publication-processing__knob" cx="70" cy="80" r="7" />
          <path class="publication-processing__body" d="M94 110 C117 110 121 132 101 144 L95 137 C110 128 108 118 94 118 Z" />
          <rect class="publication-processing__band" x="44" y="141" width="52" height="7" rx="3" />
          <path class="publication-processing__body" d="M46 147 L94 147 L101 188 L39 188 Z" />
          <path class="publication-processing__shine" d="M58 100 L64 100 L57 141 L51 141 Z" />
          <path class="publication-processing__shine" d="M51 151 L57 151 L51 185 L45 185 Z" />
        </g>
      </svg>
      <h2 id="publication-processing-title">Preparando sua publicação</h2>
      <p>Estamos analisando a foto e organizando tudo. Isso pode levar alguns segundos.</p>
      <p class="publication-processing__curiosity" aria-live="polite">
        <strong>Curiosidade de cozinha</strong>
        {{ CURIOSITIES[curiosityIndex] }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.publication-processing {
  position: fixed;
  z-index: 1100;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-5);
  background: var(--color-overlay);
  backdrop-filter: blur(3px);
}

.publication-processing__card {
  width: min(100%, 27rem);
  padding: var(--space-8) var(--space-6);
  color: var(--color-text);
  text-align: center;
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.publication-processing__pot {
  width: 7rem;
  height: 10rem;
  margin: 0 auto var(--space-2);
}

.publication-processing__body { fill: var(--color-primary); }
.publication-processing__band,
.publication-processing__knob { fill: var(--color-accent); }
.publication-processing__shine { fill: var(--color-primary-contrast); opacity: 0.16; }
.publication-processing__steam { fill: none; stroke: var(--color-text-secondary); stroke-linecap: round; stroke-width: 5; opacity: 0; }
.publication-processing__steam { animation: publication-processing-rise 2.9s ease-out infinite; transform-origin: 50% 100%; }
.publication-processing__steam--2 { animation-delay: 0.45s; }
.publication-processing__steam--3 { animation-delay: 0.95s; }
.publication-processing__pot-shape { animation: publication-processing-warm 2.9s ease-in-out infinite; transform-origin: 50% 100%; }

h2 { margin: 0 0 var(--space-2); font-size: var(--font-size-2xl); }
p { margin: 0; color: var(--color-text-secondary); line-height: var(--line-height-body); }
.publication-processing__curiosity { display: grid; gap: var(--space-1); margin-top: var(--space-6); padding-top: var(--space-4); border-top: 1px solid var(--color-border); font-size: var(--font-size-sm); }
.publication-processing__curiosity strong { color: var(--color-text); }

@keyframes publication-processing-rise {
  0% { opacity: 0; transform: translateY(0.75rem) scaleY(0.8); }
  22% { opacity: 0.75; }
  60% { opacity: 0.45; }
  100% { opacity: 0; transform: translateY(-1rem) scaleY(1.15); }
}

@keyframes publication-processing-warm { 0%, 100% { transform: scaleY(1); } 50% { transform: scaleY(1.015); } }

@media (prefers-reduced-motion: reduce) {
  .publication-processing__steam,
  .publication-processing__pot-shape { animation: none; }
  .publication-processing__steam { opacity: 0.5; }
}
</style>
