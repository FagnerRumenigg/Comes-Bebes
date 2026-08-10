<script setup lang="ts">
import type { IngredientResponse } from '@/api/generated/models'

defineProps<{ ingredients: IngredientResponse[] }>()
</script>

<template>
  <section class="ingredient-list" aria-labelledby="ingredients-title">
    <h2 id="ingredients-title">Ingredientes</h2>
    <ul>
      <li v-for="ingredient in ingredients" :key="ingredient.position">
        <span>{{ ingredient.name }}</span>
        <span v-if="ingredient.quantity !== null || ingredient.unit">
          {{ ingredient.quantity ?? 'A gosto' }} {{ ingredient.unit ?? '' }}
        </span>
        <small v-if="ingredient.note">{{ ingredient.note }}</small>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.ingredient-list {
  display: grid;
  gap: var(--space-4);
}

.ingredient-list h2 {
  margin: 0;
  font-size: var(--font-size-xl);
}

.ingredient-list ul {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.ingredient-list li {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: var(--space-3);
  padding-block-end: var(--space-3);
  border-block-end: 1px solid var(--color-border);
}

.ingredient-list li > span:nth-child(2) {
  color: var(--color-text-secondary);
  text-align: right;
}

.ingredient-list small {
  color: var(--color-text-secondary);
}

@media (max-width: 30rem) {
  .ingredient-list li {
    grid-template-columns: 1fr auto;
  }

  .ingredient-list small {
    grid-column: 1 / -1;
  }
}
</style>
