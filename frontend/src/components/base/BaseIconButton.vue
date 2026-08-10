<script setup lang="ts">
type IconButtonVariant = 'ghost' | 'secondary' | 'danger'

const props = withDefaults(
  defineProps<{
    label: string
    variant?: IconButtonVariant
    disabled?: boolean
    loading?: boolean
    pressed?: boolean
  }>(),
  {
    variant: 'ghost',
    disabled: false,
    loading: false,
    pressed: undefined,
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent): void {
  if (props.disabled || props.loading) {
    event.preventDefault()
    return
  }

  emit('click', event)
}
</script>

<template>
  <button
    class="base-icon-button"
    :class="[`base-icon-button--${variant}`, { 'base-icon-button--pressed': pressed }]"
    type="button"
    :aria-label="label"
    :aria-busy="loading || undefined"
    :aria-pressed="pressed"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="base-icon-button__spinner" aria-hidden="true" />
    <span v-else aria-hidden="true"><slot /></span>
  </button>
</template>

<style scoped>
.base-icon-button {
  display: inline-grid;
  width: var(--control-min-size);
  height: var(--control-min-size);
  place-items: center;
  padding: var(--space-2);
  color: var(--color-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  transition:
    color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard),
    opacity var(--duration-fast) var(--ease-standard);
}

.base-icon-button:hover:not(:disabled),
.base-icon-button--pressed {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.base-icon-button--secondary {
  background: var(--color-surface);
  border-color: var(--color-border);
}

.base-icon-button--danger {
  color: var(--color-danger);
}

.base-icon-button:disabled {
  opacity: 0.5;
}

.base-icon-button__spinner {
  width: 1.125rem;
  height: 1.125rem;
  border: 2px solid currentcolor;
  border-right-color: transparent;
  border-radius: var(--radius-pill);
  animation: base-icon-button-spin var(--duration-slow) linear infinite;
}

@keyframes base-icon-button-spin {
  to {
    transform: rotate(1turn);
  }
}
</style>
