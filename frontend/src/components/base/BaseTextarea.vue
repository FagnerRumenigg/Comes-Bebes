<script setup lang="ts">
import { computed, useId } from 'vue'

import BaseFieldError from './BaseFieldError.vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    id?: string
    hint?: string
    error?: string
    disabled?: boolean
    required?: boolean
    rows?: number
  }>(),
  {
    modelValue: '',
    id: undefined,
    hint: undefined,
    error: undefined,
    disabled: false,
    required: false,
    rows: 5,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const generatedId = useId()
const textareaId = computed(() => props.id ?? `textarea-${generatedId}`)
const hintId = computed(() => `${textareaId.value}-hint`)
const errorId = computed(() => `${textareaId.value}-error`)
const describedBy = computed(() => {
  if (props.error) return errorId.value
  if (props.hint) return hintId.value
  return undefined
})

function updateValue(event: Event): void {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div class="base-field">
    <label class="base-field__label" :for="textareaId">
      {{ label }}
      <span v-if="required" class="base-field__required" aria-hidden="true">*</span>
    </label>
    <textarea
      v-bind="$attrs"
      :id="textareaId"
      class="base-field__control base-field__control--textarea"
      :class="{ 'base-field__control--error': error }"
      :value="modelValue"
      :rows="rows"
      :disabled="disabled"
      :required="required"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      @input="updateValue"
    />
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
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  line-height: var(--line-height-ui);
}

.base-field__required,
.base-field__control--error {
  color: var(--color-danger);
}

.base-field__control {
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  resize: vertical;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  transition:
    border-color var(--duration-fast) var(--ease-standard),
    opacity var(--duration-fast) var(--ease-standard);
}

.base-field__control:hover:not(:disabled) {
  border-color: var(--color-text-secondary);
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
