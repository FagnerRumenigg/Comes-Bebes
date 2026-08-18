<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { useSearchTags } from '@/api/generated/tags/tags'

const MAX_TAGS = 5

const props = defineProps<{ modelValue: string[]; error?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>()

const query = ref('')
const debouncedQuery = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | undefined

watch(query, (value) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedQuery.value = value
  }, 300)
})

function normalize(value: string): string {
  return value.trim().toLowerCase()
}

const atLimit = computed(() => props.modelValue.length >= MAX_TAGS)
const searchEnabled = computed(() => !atLimit.value && debouncedQuery.value.trim().length > 0)

const suggestionsQuery = useSearchTags(
  computed(() => ({ q: debouncedQuery.value })),
  { query: { enabled: searchEnabled } },
)

const suggestions = computed(() =>
  (suggestionsQuery.data.value ?? []).filter(
    (tag) => !props.modelValue.some((existing) => normalize(existing) === normalize(tag.name)),
  ),
)

function addTag(name: string): void {
  const trimmed = name.trim()
  if (!trimmed || atLimit.value) return
  if (props.modelValue.some((existing) => normalize(existing) === normalize(trimmed))) return
  emit('update:modelValue', [...props.modelValue, trimmed])
  query.value = ''
  debouncedQuery.value = ''
}

function removeTag(index: number): void {
  emit(
    'update:modelValue',
    props.modelValue.filter((_, itemIndex) => itemIndex !== index),
  )
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' || event.key === ',') {
    event.preventDefault()
    addTag(query.value)
  }
}
</script>

<template>
  <fieldset class="tag-editor">
    <legend>Tags</legend>
    <p class="tag-editor__hint">
      Até 5 tags para ajudar outras pessoas a encontrar sua publicação.
    </p>
    <ul v-if="modelValue.length" class="tag-editor__chips">
      <li v-for="(tag, index) in modelValue" :key="tag">
        {{ tag }}
        <button type="button" :aria-label="`Remover tag ${tag}`" @click="removeTag(index)">
          ×
        </button>
      </li>
    </ul>
    <div class="tag-editor__input-wrap">
      <label>
        <span>Adicionar tag</span>
        <input
          v-model="query"
          type="text"
          maxlength="40"
          :disabled="atLimit"
          :placeholder="atLimit ? 'Limite de 5 tags atingido' : 'Digite e pressione Enter'"
          @keydown="handleKeydown"
        />
      </label>
      <ul v-if="suggestions.length" class="tag-editor__suggestions">
        <li v-for="suggestion in suggestions" :key="suggestion.slug">
          <button type="button" @click="addTag(suggestion.name)">
            {{ suggestion.name }}
            <span v-if="suggestion.official" class="tag-editor__official">Oficial</span>
          </button>
        </li>
      </ul>
    </div>
    <p v-if="error" class="tag-editor__error" role="alert">{{ error }}</p>
  </fieldset>
</template>

<style scoped>
.tag-editor {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  border: 0;
}
.tag-editor legend {
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}
.tag-editor__hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.tag-editor__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}
.tag-editor__chips li {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}
.tag-editor__chips button {
  color: inherit;
  font-weight: var(--font-weight-bold);
  background: transparent;
  border: 0;
  cursor: pointer;
}
.tag-editor__input-wrap {
  position: relative;
}
.tag-editor label {
  display: grid;
  gap: var(--space-2);
}
.tag-editor label span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}
.tag-editor input {
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.tag-editor__suggestions {
  position: absolute;
  z-index: 10;
  top: 100%;
  left: 0;
  display: grid;
  width: 100%;
  margin-block-start: var(--space-1);
  padding: var(--space-1);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
}
.tag-editor__suggestions button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: var(--control-min-size);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text);
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.tag-editor__suggestions button:hover {
  background: var(--color-surface-raised);
}
.tag-editor__official {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
}
.tag-editor__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}
</style>
