<script setup lang="ts">
import { ref } from 'vue'

import BaseInput from '@/components/base/BaseInput.vue'
import AppIcon from '@/components/icons/AppIcon.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    id?: string
    hint?: string
    error?: string
    invalid?: boolean
    disabled?: boolean
    required?: boolean
  }>(),
  {
    modelValue: '',
    id: undefined,
    hint: undefined,
    error: undefined,
    invalid: false,
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
    :invalid="invalid"
    :disabled="disabled"
    :required="required"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #lead>
      <AppIcon name="lock" :size="20" />
    </template>
    <template #trailing>
      <button
        class="password-input__toggle"
        type="button"
        :aria-label="visible ? 'Ocultar senha' : 'Mostrar senha'"
        :aria-pressed="visible"
        :disabled="disabled"
        @click="visible = !visible"
      >
        <AppIcon name="eye" :size="20" />
      </button>
    </template>
  </BaseInput>
</template>

<style scoped>
.password-input__toggle {
  display: grid;
  width: 2.25rem;
  height: 2.25rem;
  place-items: center;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
}

.password-input__toggle[aria-pressed='true'] {
  color: var(--color-primary);
}
</style>
