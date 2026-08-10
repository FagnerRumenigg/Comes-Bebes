<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

import BaseIconButton from './BaseIconButton.vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    closeLabel?: string
  }>(),
  {
    description: undefined,
    closeLabel: 'Fechar diálogo',
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
  close: []
}>()

const dialog = ref<HTMLDialogElement>()
const titleId = `dialog-title-${useId()}`
const descriptionId = `dialog-description-${useId()}`
let previouslyFocusedElement: HTMLElement | null = null

function requestClose(): void {
  emit('update:open', false)
}

function handleCancel(event: Event): void {
  event.preventDefault()
  requestClose()
}

function handleBackdropClick(event: MouseEvent): void {
  if (event.target === dialog.value) requestClose()
}

function handleClosed(): void {
  emit('close')
  void nextTick(() => previouslyFocusedElement?.focus())
}

watch(
  () => props.open,
  async (isOpen) => {
    await nextTick()
    const element = dialog.value
    if (!element) return

    if (isOpen && !element.open) {
      previouslyFocusedElement =
        document.activeElement instanceof HTMLElement ? document.activeElement : null
      element.showModal()
    } else if (!isOpen && element.open) {
      element.close()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (dialog.value?.open) dialog.value.close()
})
</script>

<template>
  <dialog
    ref="dialog"
    class="base-dialog"
    :aria-labelledby="titleId"
    :aria-describedby="description ? descriptionId : undefined"
    @cancel="handleCancel"
    @close="handleClosed"
    @click="handleBackdropClick"
  >
    <div class="base-dialog__surface">
      <header class="base-dialog__header">
        <div>
          <h2 :id="titleId" class="base-dialog__title">{{ title }}</h2>
          <p v-if="description" :id="descriptionId" class="base-dialog__description">
            {{ description }}
          </p>
        </div>
        <BaseIconButton :label="closeLabel" @click="requestClose">×</BaseIconButton>
      </header>

      <div class="base-dialog__content">
        <slot />
      </div>

      <footer v-if="$slots.actions" class="base-dialog__actions">
        <slot name="actions" />
      </footer>
    </div>
  </dialog>
</template>

<style scoped>
.base-dialog {
  width: min(calc(100% - var(--space-8)), 36rem);
  max-height: min(calc(100% - var(--space-8)), 48rem);
  padding: 0;
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.base-dialog::backdrop {
  background: var(--color-overlay);
  backdrop-filter: blur(2px);
}

.base-dialog__surface {
  display: grid;
  gap: var(--space-6);
  padding: var(--space-6);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: inherit;
}

.base-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.base-dialog__title {
  font-size: var(--font-size-2xl);
}

.base-dialog__description {
  margin-block-start: var(--space-2);
  color: var(--color-text-secondary);
}

.base-dialog__content {
  min-width: 0;
}

.base-dialog__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
}

@media (max-width: 30rem) {
  .base-dialog {
    width: min(calc(100% - var(--space-4)), 36rem);
  }

  .base-dialog__surface {
    padding: var(--space-5);
  }
}
</style>
