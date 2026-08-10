<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ instructions: string }>()
const steps = computed(() =>
  props.instructions
    .split(/\r?\n/)
    .map((step) => step.trim())
    .filter(Boolean),
)
</script>

<template>
  <section class="recipe-instructions" aria-labelledby="instructions-title">
    <h2 id="instructions-title">Modo de preparo</h2>
    <ol>
      <li v-for="(step, index) in steps" :key="`${index}-${step}`">{{ step }}</li>
    </ol>
  </section>
</template>

<style scoped>
.recipe-instructions {
  display: grid;
  gap: var(--space-4);
}

.recipe-instructions h2 {
  margin: 0;
  font-size: var(--font-size-xl);
}

.recipe-instructions ol {
  display: grid;
  gap: var(--space-4);
  margin: 0;
  padding-inline-start: var(--space-6);
}

.recipe-instructions li {
  padding-inline-start: var(--space-2);
  line-height: var(--line-height-body);
}

.recipe-instructions li::marker {
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}
</style>
