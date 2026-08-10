<script setup lang="ts">
import { nextTick, ref, useId, watch } from 'vue'

const props = defineProps<{
  modelValue: string
  error?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const groupId = `preparation-${useId()}`
const steps = ref(toSteps(props.modelValue))

function toSteps(value: string): string[] {
  const parsed = value.split(/\r?\n/)
  return parsed.length ? parsed : ['']
}

function emitSteps(): void {
  emit('update:modelValue', steps.value.join('\n'))
}

function focusStep(index: number): void {
  void nextTick(() => document.getElementById(`${groupId}-step-${index}`)?.focus())
}

function update(index: number, value: string): void {
  const pastedSteps = value.split(/\r?\n/)
  steps.value.splice(index, 1, ...pastedSteps)
  emitSteps()
}

function addAfter(index: number): void {
  steps.value.splice(index + 1, 0, '')
  emitSteps()
  focusStep(index + 1)
}

function remove(index: number): void {
  if (steps.value.length === 1) {
    steps.value[0] = ''
    emitSteps()
    return
  }
  steps.value.splice(index, 1)
  emitSteps()
  focusStep(Math.max(0, index - 1))
}

function removeEmptyWithBackspace(index: number): void {
  if (!steps.value[index] && steps.value.length > 1) remove(index)
}

watch(
  () => props.modelValue,
  (value) => {
    if (value !== steps.value.join('\n')) steps.value = toSteps(value)
  },
)
</script>

<template>
  <fieldset class="preparation-steps">
    <legend>Modo de preparo <span aria-hidden="true">*</span></legend>
    <p class="preparation-steps__hint">
      Escreva um passo por vez. Pressione Enter para criar o próximo.
    </p>
    <div v-for="(step, index) in steps" :key="index" class="preparation-steps__row">
      <label :for="`${groupId}-step-${index}`">Passo {{ index + 1 }}</label>
      <textarea
        :id="`${groupId}-step-${index}`"
        :value="step"
        rows="2"
        maxlength="2000"
        :required="index === 0"
        @input="update(index, ($event.target as HTMLTextAreaElement).value)"
        @keydown.enter.prevent="addAfter(index)"
        @keydown.backspace="removeEmptyWithBackspace(index)"
      />
      <button
        type="button"
        :aria-label="`Remover passo ${index + 1}`"
        :disabled="steps.length === 1 && !step"
        @click="remove(index)"
      >
        Remover
      </button>
    </div>
    <p v-if="error" class="preparation-steps__error" role="alert">{{ error }}</p>
    <button type="button" class="preparation-steps__add" @click="addAfter(steps.length - 1)">
      + Adicionar passo
    </button>
  </fieldset>
</template>

<style scoped>
.preparation-steps {
  display: grid;
  gap: var(--space-4);
  padding: 0;
  border: 0;
}

.preparation-steps legend {
  font-family: var(--font-editorial);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.preparation-steps legend span,
.preparation-steps__error {
  color: var(--color-danger);
}

.preparation-steps__hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.preparation-steps__row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: var(--space-2) var(--space-3);
}

.preparation-steps__row label {
  grid-column: 1 / -1;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.preparation-steps__row textarea {
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-3);
  color: var(--color-text);
  resize: vertical;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.preparation-steps__row button,
.preparation-steps__add {
  min-height: 2.25rem;
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.preparation-steps__row button:disabled {
  opacity: 0.45;
}

.preparation-steps__error {
  margin: 0;
  font-size: var(--font-size-sm);
}

.preparation-steps__add {
  width: fit-content;
}

@media (max-width: 30rem) {
  .preparation-steps__row {
    grid-template-columns: 1fr;
  }

  .preparation-steps__row button {
    width: fit-content;
  }
}
</style>
