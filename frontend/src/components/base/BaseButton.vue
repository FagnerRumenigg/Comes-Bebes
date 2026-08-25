<script setup lang="ts">
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'

const props = withDefaults(
  defineProps<{
    type?: 'button' | 'submit' | 'reset'
    variant?: ButtonVariant
    disabled?: boolean
    loading?: boolean
    pressed?: boolean
  }>(),
  {
    type: 'button',
    variant: 'primary',
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
    class="base-button"
    :class="[`base-button--${variant}`, { 'base-button--pressed': pressed }]"
    :type="type"
    :disabled="disabled"
    :aria-disabled="loading || undefined"
    :aria-busy="loading || undefined"
    :aria-pressed="pressed"
    @click="handleClick"
  >
    <span v-if="loading" class="base-button__spinner" aria-hidden="true" />
    <span class="base-button__content" :class="{ 'base-button__content--hidden': loading }">
      <slot />
    </span>
  </button>
</template>

<style scoped>
.base-button {
  position: relative;
  display: inline-flex;
  min-height: var(--control-min-size);
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  line-height: var(--line-height-ui);
  background: var(--color-primary);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
  transition:
    background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard),
    color var(--duration-fast) var(--ease-standard),
    opacity var(--duration-fast) var(--ease-standard),
    transform var(--duration-instant) var(--ease-standard);
}

.base-button:hover:not(:disabled) {
  filter: brightness(1.08);
}

.base-button:active:not(:disabled) {
  transform: translateY(1px);
}

.base-button--secondary {
  color: var(--color-text);
  background: var(--color-surface);
  border-color: var(--color-border);
}

.base-button--ghost {
  color: var(--color-primary);
  background: transparent;
  border-color: transparent;
  box-shadow: none;
}

.base-button--danger {
  color: var(--color-danger);
  background: transparent;
  border-color: var(--color-danger);
  box-shadow: none;
}

.base-button--pressed {
  background: color-mix(in srgb, var(--color-primary) 18%, var(--color-surface));
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.base-button:disabled,
.base-button[aria-busy='true'] {
  opacity: 0.55;
}

.base-button__content {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: inherit;
}

.base-button__content--hidden {
  visibility: hidden;
}

.base-button__spinner {
  position: absolute;
  width: 1.125rem;
  height: 1.125rem;
  border: 2px solid currentcolor;
  border-right-color: transparent;
  border-radius: var(--radius-pill);
  animation: base-button-spin var(--duration-slow) linear infinite;
}

@keyframes base-button-spin {
  to {
    transform: rotate(1turn);
  }
}
</style>
