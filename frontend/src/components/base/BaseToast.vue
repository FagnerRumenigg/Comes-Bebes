<script setup lang="ts">
import BaseIconButton from './BaseIconButton.vue'

type ToastKind = 'info' | 'success' | 'error'

withDefaults(
  defineProps<{
    title: string
    kind?: ToastKind
    dismissible?: boolean
    dismissLabel?: string
  }>(),
  {
    kind: 'info',
    dismissible: true,
    dismissLabel: 'Dispensar aviso',
  },
)

defineEmits<{
  dismiss: []
}>()
</script>

<template>
  <section
    class="base-toast"
    :class="`base-toast--${kind}`"
    :role="kind === 'error' ? 'alert' : 'status'"
    :aria-live="kind === 'error' ? 'assertive' : 'polite'"
    aria-atomic="true"
  >
    <div class="base-toast__content">
      <strong class="base-toast__title">{{ title }}</strong>
      <div v-if="$slots.default" class="base-toast__message"><slot /></div>
    </div>
    <BaseIconButton v-if="dismissible" :label="dismissLabel" @click="$emit('dismiss')">
      ×
    </BaseIconButton>
  </section>
</template>

<style scoped>
.base-toast {
  display: flex;
  width: min(100%, 28rem);
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  color: var(--color-text);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-secondary);
  border-inline-start-width: var(--space-1);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.base-toast--success {
  border-color: var(--color-success);
}

.base-toast--error {
  border-color: var(--color-danger);
}

.base-toast__content {
  display: grid;
  gap: var(--space-1);
}

.base-toast__title {
  font-weight: var(--font-weight-semibold);
  line-height: var(--line-height-ui);
}

.base-toast__message {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-ui);
}
</style>
