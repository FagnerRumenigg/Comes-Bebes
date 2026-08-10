<script setup lang="ts">
export interface IngredientDraft {
  name: string
  quantity: string
  unit: string
  note: string
}

const props = defineProps<{ modelValue: IngredientDraft[]; error?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: IngredientDraft[]] }>()

function update(index: number, field: keyof IngredientDraft, value: string): void {
  const next = props.modelValue.map((item, itemIndex) =>
    itemIndex === index ? { ...item, [field]: value } : item,
  )
  emit('update:modelValue', next)
}

function add(): void {
  emit('update:modelValue', [...props.modelValue, { name: '', quantity: '', unit: '', note: '' }])
}

function remove(index: number): void {
  if (props.modelValue.length === 1) return
  emit(
    'update:modelValue',
    props.modelValue.filter((_, itemIndex) => itemIndex !== index),
  )
}

function move(index: number, direction: -1 | 1): void {
  const target = index + direction
  if (target < 0 || target >= props.modelValue.length) return
  const next = [...props.modelValue]
  ;[next[index], next[target]] = [next[target], next[index]]
  emit('update:modelValue', next)
}
</script>

<template>
  <fieldset class="ingredient-editor">
    <legend>Ingredientes</legend>
    <p class="ingredient-editor__hint">
      Inclua pelo menos um ingrediente e ordene como aparece na receita.
    </p>
    <div v-for="(ingredient, index) in modelValue" :key="index" class="ingredient-editor__row">
      <label>
        <span>Ingrediente {{ index + 1 }}</span>
        <input
          :value="ingredient.name"
          maxlength="150"
          required
          @input="update(index, 'name', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label>
        <span>Quantidade</span>
        <input
          :value="ingredient.quantity"
          inputmode="decimal"
          @input="update(index, 'quantity', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label>
        <span>Unidade</span>
        <input
          :value="ingredient.unit"
          maxlength="50"
          placeholder="g, xícara..."
          @input="update(index, 'unit', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label>
        <span>Observação</span>
        <input
          :value="ingredient.note"
          maxlength="255"
          placeholder="Opcional"
          @input="update(index, 'note', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <div class="ingredient-editor__actions">
        <button
          type="button"
          :disabled="index === 0"
          aria-label="Mover ingrediente para cima"
          @click="move(index, -1)"
        >
          ↑
        </button>
        <button
          type="button"
          :disabled="index === modelValue.length - 1"
          aria-label="Mover ingrediente para baixo"
          @click="move(index, 1)"
        >
          ↓
        </button>
        <button
          type="button"
          :disabled="modelValue.length === 1"
          aria-label="Remover ingrediente"
          @click="remove(index)"
        >
          Remover
        </button>
      </div>
    </div>
    <p v-if="error" class="ingredient-editor__error" role="alert">{{ error }}</p>
    <button type="button" class="ingredient-editor__add" @click="add">
      + Adicionar ingrediente
    </button>
  </fieldset>
</template>

<style scoped>
.ingredient-editor {
  display: grid;
  gap: var(--space-4);
  padding: 0;
  border: 0;
}
.ingredient-editor legend {
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}
.ingredient-editor__hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.ingredient-editor__row {
  display: grid;
  grid-template-columns:
    minmax(10rem, 2fr) minmax(6rem, 1fr) minmax(7rem, 1fr) minmax(10rem, 2fr)
    auto;
  gap: var(--space-3);
  align-items: end;
  padding-block-end: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}
.ingredient-editor label {
  display: grid;
  gap: var(--space-2);
}
.ingredient-editor label span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}
.ingredient-editor input {
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.ingredient-editor__actions {
  display: flex;
  gap: var(--space-1);
}
.ingredient-editor__actions button,
.ingredient-editor__add {
  min-height: 2.25rem;
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.ingredient-editor__actions button:disabled {
  opacity: 0.45;
}
.ingredient-editor__error {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}
@media (max-width: 48rem) {
  .ingredient-editor__row {
    grid-template-columns: 1fr 1fr;
  }
  .ingredient-editor__row label:first-child,
  .ingredient-editor__row label:nth-child(4),
  .ingredient-editor__actions {
    grid-column: 1 / -1;
  }
}
</style>
