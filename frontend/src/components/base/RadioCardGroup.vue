<script setup lang="ts">
defineProps<{
  modelValue: string
  options: Array<{ value: string; title: string; description: string }>
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <div class="radio-card-group" role="radiogroup">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="radio-card-group__opt"
      :aria-checked="modelValue === option.value"
      role="radio"
      :disabled="disabled"
      @click="emit('update:modelValue', option.value)"
    >
      <span class="radio-card-group__radio" aria-hidden="true" />
      <span>
        <b>{{ option.title }}</b>
        <span class="radio-card-group__description">{{ option.description }}</span>
      </span>
    </button>
  </div>
</template>

<style scoped>
.radio-card-group {
  display: grid;
  gap: var(--space-2);
}

.radio-card-group__opt {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition:
    border-color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard);
}

.radio-card-group__opt[aria-checked='true'] {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  border-color: var(--color-primary);
}

.radio-card-group__opt:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.radio-card-group__radio {
  display: grid;
  width: 1.375rem;
  height: 1.375rem;
  flex: none;
  margin-top: 0.125rem;
  place-items: center;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.radio-card-group__opt[aria-checked='true'] .radio-card-group__radio {
  border-color: var(--color-primary);
}

.radio-card-group__opt[aria-checked='true'] .radio-card-group__radio::after {
  width: 0.6875rem;
  height: 0.6875rem;
  background: var(--color-primary);
  border-radius: var(--radius-pill);
  content: '';
}

.radio-card-group__opt b {
  display: block;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}

.radio-card-group__description {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
}
</style>
