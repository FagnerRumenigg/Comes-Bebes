<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'

import type { PublicationResponse } from '@/api/generated/models'

import PublicationImage from './PublicationImage.vue'

defineProps<{
  publication: PublicationResponse
}>()

const flipped = ref(false)
const transitionLocked = ref(false)
let unlockTimer: ReturnType<typeof setTimeout> | undefined

function toggle(): void {
  if (transitionLocked.value) return
  transitionLocked.value = true
  flipped.value = !flipped.value
  unlockTimer = setTimeout(() => {
    transitionLocked.value = false
    unlockTimer = undefined
  }, 450)
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter' && event.key !== ' ') return
  event.preventDefault()
  toggle()
}

onBeforeUnmount(() => {
  if (unlockTimer) clearTimeout(unlockTimer)
})
</script>

<template>
  <div class="recipe-flip" :class="{ 'recipe-flip--flipped': flipped }">
    <div class="recipe-flip__surface">
      <div
        class="recipe-flip__face recipe-flip__front"
        role="button"
        :tabindex="flipped ? -1 : 0"
        aria-label="Mostrar prévia da receita"
        :aria-pressed="flipped"
        :aria-disabled="transitionLocked"
        @click="toggle"
        @keydown="handleKeydown"
      >
        <PublicationImage
          :src="publication.imageUrl"
          :alt="publication.title ?? `Receita de ${publication.authorDisplayName}`"
        />
        <span class="recipe-flip__affordance">Toque para ver a receita</span>
      </div>

      <div class="recipe-flip__face recipe-flip__back">
        <button
          type="button"
          class="recipe-flip__back-toggle"
          aria-label="Voltar para a foto"
          :tabindex="flipped ? 0 : -1"
          :disabled="transitionLocked"
          @click="toggle"
        />
        <div class="recipe-flip__back-content">
          <span class="recipe-flip__eyebrow">Prévia da receita</span>
          <h3>{{ publication.title ?? 'Receita sem título' }}</h3>
          <p v-if="publication.recipePreview?.yieldQuantity">
            Rende {{ publication.recipePreview.yieldQuantity }}
            {{ publication.recipePreview.yieldUnit ?? 'porções' }}
          </p>
          <ul v-if="publication.recipePreview" class="recipe-flip__ingredients">
            <li
              v-for="ingredient in publication.recipePreview.ingredients.slice(0, 3)"
              :key="ingredient.position"
            >
              {{ ingredient.name }}
            </li>
            <li v-if="publication.recipePreview.ingredients.length > 3">
              + {{ publication.recipePreview.ingredients.length - 3 }} ingredientes
            </li>
          </ul>
          <RouterLink
            class="recipe-flip__link"
            :to="`/publicacoes/${publication.id}`"
            @click.stop
            @keydown.stop
          >
            Ver receita completa <span aria-hidden="true">→</span>
          </RouterLink>
          <span class="recipe-flip__affordance recipe-flip__affordance--back">
            Toque para voltar à foto
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recipe-flip {
  perspective: 1000px;
}

.recipe-flip__surface {
  position: relative;
  min-height: 34rem;
  cursor: pointer;
  transform-style: preserve-3d;
  transition: transform 450ms var(--ease-emphasized);
}

.recipe-flip--flipped .recipe-flip__surface {
  transform: rotateY(180deg);
}

.recipe-flip__face {
  position: absolute;
  inset: 0;
  overflow: hidden;
  backface-visibility: hidden;
  border-radius: inherit;
  pointer-events: none;
}

.recipe-flip__front {
  background: var(--color-surface);
  pointer-events: auto;
}

.recipe-flip--flipped .recipe-flip__front {
  pointer-events: none;
}

.recipe-flip--flipped .recipe-flip__back {
  pointer-events: auto;
}

.recipe-flip__front :deep(.publication-image) {
  height: 100%;
  aspect-ratio: auto;
}

.recipe-flip__affordance {
  position: absolute;
  right: var(--space-4);
  bottom: var(--space-4);
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary-contrast);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: color-mix(in srgb, var(--color-text) 76%, transparent);
  border-radius: var(--radius-pill);
}

.recipe-flip__back {
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  transform: rotateY(180deg);
}

.recipe-flip__back-toggle {
  position: absolute;
  z-index: 0;
  inset: 0;
  width: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.recipe-flip__back-toggle:disabled {
  cursor: wait;
}

.recipe-flip__back-content {
  position: relative;
  z-index: 1;
  display: grid;
  height: 100%;
  align-content: center;
  gap: var(--space-4);
  padding: var(--space-8);
  pointer-events: none;
}

.recipe-flip__back-content .recipe-flip__link {
  pointer-events: auto;
}

.recipe-flip__eyebrow {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.recipe-flip__back h3 {
  margin: 0;
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
}

.recipe-flip__back p {
  margin: 0;
  color: var(--color-text-secondary);
}

.recipe-flip__ingredients {
  display: grid;
  gap: var(--space-2);
  margin: 0;
  padding-inline-start: var(--space-5);
  color: var(--color-text-secondary);
}

.recipe-flip__link {
  width: fit-content;
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
}

.recipe-flip__link:hover {
  text-decoration: underline;
  text-underline-offset: 0.2em;
}

.recipe-flip__affordance--back {
  position: static;
  width: fit-content;
  color: var(--color-text-secondary);
  background: transparent;
}

@media (prefers-reduced-motion: reduce) {
  .recipe-flip__surface {
    transition: opacity var(--duration-fast) var(--ease-standard);
    transform: none;
  }

  .recipe-flip__face {
    transition: opacity var(--duration-fast) var(--ease-standard);
    transform: none;
  }

  .recipe-flip:not(.recipe-flip--flipped) .recipe-flip__back,
  .recipe-flip--flipped .recipe-flip__front {
    opacity: 0;
  }
}

@media (max-width: 30rem) {
  .recipe-flip__surface {
    min-height: 26rem;
  }

  .recipe-flip__back-content {
    padding: var(--space-6);
  }
}
</style>
