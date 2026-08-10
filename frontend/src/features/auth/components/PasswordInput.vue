<script setup lang="ts">
import { ref } from 'vue'

import BaseInput from '@/components/base/BaseInput.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    id?: string
    hint?: string
    error?: string
    disabled?: boolean
    required?: boolean
  }>(),
  {
    modelValue: '',
    id: undefined,
    hint: undefined,
    error: undefined,
    disabled: false,
    required: false,
  },
)

defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)
</script>

<template>
  <BaseInput
    v-bind="$attrs"
    :id="id"
    :model-value="modelValue"
    :label="label"
    :type="visible ? 'text' : 'password'"
    :hint="hint"
    :error="error"
    :disabled="disabled"
    :required="required"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #trailing>
      <button
        class="password-input__toggle"
        type="button"
        :aria-label="visible ? 'Ocultar senha' : 'Mostrar senha'"
        :aria-pressed="visible"
        :disabled="disabled"
        @click="visible = !visible"
      >
        {{ visible ? 'Ocultar' : 'Mostrar' }}
      </button>
    </template>
  </BaseInput>
</template>

<style scoped>
.password-input__toggle {
  min-height: 2rem;
  padding-inline: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
}
</style>
