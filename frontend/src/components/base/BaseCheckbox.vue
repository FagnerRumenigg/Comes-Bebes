<script setup lang="ts">
import { computed, useId } from 'vue'

import BaseFieldError from './BaseFieldError.vue'

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    label: string
    id?: string
    description?: string
    error?: string
    disabled?: boolean
    required?: boolean
  }>(),
  {
    modelValue: false,
    id: undefined,
    description: undefined,
    error: undefined,
    disabled: false,
    required: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const generatedId = useId()
const checkboxId = computed(() => props.id ?? `checkbox-${generatedId}`)
const descriptionId = computed(() => `${checkboxId.value}-description`)
const errorId = computed(() => `${checkboxId.value}-error`)
const describedBy = computed(() => {
  if (props.error) return errorId.value
  if (props.description) return descriptionId.value
  return undefined
})

function updateValue(event: Event): void {
  emit('update:modelValue', (event.target as HTMLInputElement).checked)
}
</script>

<template>
  <div class="base-checkbox">
    <div class="base-checkbox__row">
      <input
        :id="checkboxId"
        class="base-checkbox__control"
        type="checkbox"
        :checked="modelValue"
        :disabled="disabled"
        :required="required"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        @change="updateValue"
      />
      <label class="base-checkbox__label" :for="checkboxId">
        {{ label }}
        <span v-if="required" class="base-checkbox__required" aria-hidden="true">*</span>
      </label>
    </div>
    <BaseFieldError v-if="error" :id="errorId" :message="error" />
    <p v-else-if="description" :id="descriptionId" class="base-checkbox__description">
      {{ description }}
    </p>
  </div>
</template>

<style scoped>
.base-checkbox {
  display: grid;
  gap: var(--space-2);
}

.base-checkbox__row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.base-checkbox__control {
  width: 1.125rem;
  height: 1.125rem;
  flex: 0 0 auto;
  margin: 0.15em 0 0;
  accent-color: var(--color-primary);
}

.base-checkbox__label {
  line-height: var(--line-height-ui);
}

.base-checkbox__required {
  color: var(--color-danger);
}

.base-checkbox__description {
  padding-inline-start: calc(1.125rem + var(--space-3));
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-ui);
}

.base-checkbox:has(.base-checkbox__control:disabled) {
  opacity: 0.55;
}
</style>
