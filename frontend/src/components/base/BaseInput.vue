<script setup lang="ts">
import { computed, useId } from 'vue'

import BaseFieldError from './BaseFieldError.vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    id?: string
    type?: string
    hint?: string
    error?: string
    invalid?: boolean
    disabled?: boolean
    required?: boolean
  }>(),
  {
    modelValue: '',
    id: undefined,
    type: 'text',
    hint: undefined,
    error: undefined,
    invalid: false,
    disabled: false,
    required: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const generatedId = useId()
const inputId = computed(() => props.id ?? `input-${generatedId}`)
const hintId = computed(() => `${inputId.value}-hint`)
const errorId = computed(() => `${inputId.value}-error`)
const describedBy = computed(() => {
  if (props.error) return errorId.value
  if (props.hint) return hintId.value
  return undefined
})

function updateValue(event: Event): void {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="base-field">
    <label class="base-field__label" :for="inputId">
      {{ label }}
      <span v-if="required" class="base-field__required" aria-hidden="true">*</span>
    </label>
    <div class="base-field__control-wrapper">
      <div v-if="$slots.lead" class="base-field__lead" aria-hidden="true">
        <slot name="lead" />
      </div>
      <input
        v-bind="$attrs"
        :id="inputId"
        class="base-field__control"
        :class="{ 'base-field__control--error': error || invalid, 'base-field__control--lead': $slots.lead }"
        :value="modelValue"
        :type="type"
        :disabled="disabled"
        :required="required"
        :aria-invalid="error || invalid ? 'true' : undefined"
        :aria-describedby="describedBy"
        @input="updateValue"
      />
      <div v-if="$slots.trailing" class="base-field__trailing">
        <slot name="trailing" />
      </div>
    </div>
    <BaseFieldError v-if="error" :id="errorId" :message="error" />
    <p v-else-if="hint" :id="hintId" class="base-field__hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.base-field {
  display: grid;
  gap: var(--space-2);
}

.base-field__label {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  line-height: var(--line-height-ui);
}

.base-field__required,
.base-field__control--error {
  color: var(--color-danger);
}

.base-field__control-wrapper {
  position: relative;
}

.base-field__control {
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  transition:
    border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard),
    opacity var(--duration-fast) var(--ease-standard);
}

.base-field__control-wrapper:has(.base-field__trailing) .base-field__control {
  padding-inline-end: 3rem;
}

.base-field__control--lead {
  padding-inline-start: 2.875rem;
}

.base-field__lead {
  position: absolute;
  top: 50%;
  left: var(--space-3);
  color: var(--color-text-secondary);
  transform: translateY(-50%);
}

.base-field__trailing {
  position: absolute;
  top: 50%;
  right: var(--space-2);
  transform: translateY(-50%);
}

.base-field__control:hover:not(:disabled) {
  border-color: var(--color-text-secondary);
}

.base-field__control:focus-visible {
  border-color: var(--color-focus);
}

.base-field__control--error {
  border-color: currentcolor;
}

.base-field__control:disabled {
  opacity: 0.55;
}

.base-field__hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-ui);
}
</style>
