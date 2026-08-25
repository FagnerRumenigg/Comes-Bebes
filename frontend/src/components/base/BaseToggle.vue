<script setup lang="ts">
import { useId } from 'vue'

import AppIcon from '@/components/icons/AppIcon.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    label: string
    hideLabel?: boolean
    disabled?: boolean
  }>(),
  {
    hideLabel: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const id = useId()

function toggle(): void {
  if (props.disabled) return
  emit('update:modelValue', !props.modelValue)
}
</script>

<template>
  <label class="base-toggle" :for="id">
    <span v-if="!hideLabel" class="base-toggle__label">{{ label }}</span>
    <button
      :id="id"
      type="button"
      class="base-toggle__control"
      role="switch"
      :aria-checked="modelValue"
      :aria-label="hideLabel ? label : undefined"
      :disabled="disabled"
      @click="toggle"
    >
      <AppIcon name="check" :size="14" :stroke-width="3.4" class="base-toggle__check" />
    </button>
  </label>
</template>

<style scoped>
.base-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  cursor: pointer;
}

.base-toggle__label {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.base-toggle__control {
  position: relative;
  flex: none;
  width: 3.375rem;
  height: 1.875rem;
  padding: 0;
  cursor: pointer;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-pill);
  transition: background-color var(--duration-fast) var(--ease-standard);
}

.base-toggle__control::after {
  position: absolute;
  top: 0.1875rem;
  left: 0.1875rem;
  width: 1.375rem;
  height: 1.375rem;
  background: var(--color-border);
  border-radius: var(--radius-pill);
  transition: left var(--duration-fast) var(--ease-standard);
  content: '';
}

.base-toggle__control[aria-checked='true'] {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.base-toggle__control[aria-checked='true']::after {
  left: 1.5rem;
  background: var(--color-primary-contrast);
}

.base-toggle__control:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.base-toggle__check {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  color: var(--color-primary-contrast);
  opacity: 0;
}

.base-toggle__control[aria-checked='true'] .base-toggle__check {
  opacity: 1;
}
</style>
