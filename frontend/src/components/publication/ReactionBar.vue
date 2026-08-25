<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { applyReaction, removeReaction } from '@/api/generated/publications/publications'
import type { PublicationResponse, ReactionRequestReactionCode } from '@/api/generated/models'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{ publication: PublicationResponse }>()

const MAX_REACTIONS = 3

// Catálogo fechado de 9, na ordem canônica — nunca reordenar (impl10.md v10 §15.1).
const REACTIONS: Array<{ code: ReactionRequestReactionCode; label: string }> = [
  { code: 'HUNGRY', label: 'Me deu fome' },
  { code: 'BEAUTIFUL', label: 'Ficou lindo' },
  { code: 'PERFECT_COMBO', label: 'Combinação perfeita' },
  { code: 'NEVER_TRIED', label: 'Nunca provei' },
  { code: 'WANT_TO_TRY', label: 'Quero tentar' },
  { code: 'SUNDAY_LUNCH_VIBES', label: 'Cara de almoço de domingo' },
  { code: 'GOES_WITH_COFFEE', label: 'Isso com café...' },
  { code: 'DANGEROUSLY_GOOD', label: 'Perigoso de bom' },
  { code: 'NEED_RECIPE', label: 'Preciso da receita!' },
]

const authStore = useAuthStore()
const queryClient = useQueryClient()
const selected = ref<ReactionRequestReactionCode[]>([...props.publication.selectedReactions])
const errorMessage = ref<string | null>(null)
const limitMessage = ref<string | null>(null)
const pickerOpen = ref(false)
const isOwnPublication = computed(() => authStore.identity?.userId === props.publication.authorId)
const atLimit = computed(() => selected.value.length >= MAX_REACTIONS)

// Reação com zero uso não aparece; nenhum número em lugar nenhum, só presença
// na ordem canônica (produto5.md v5 §3.1, impl10.md v10 §15.2).
const usedReactions = computed(() =>
  REACTIONS.filter((reaction) => props.publication.usedReactions.includes(reaction.code)),
)

watch(
  () => props.publication.selectedReactions,
  (value) => {
    selected.value = [...value]
  },
  { deep: true },
)

const mutation = useMutation({
  mutationFn: ({ code, active }: { code: ReactionRequestReactionCode; active: boolean }) =>
    active
      ? applyReaction(props.publication.id, { reactionCode: code })
      : removeReaction(props.publication.id, { reactionCode: code }),
  onSuccess: () => {
    void queryClient.invalidateQueries({ queryKey: ['publications'] })
  },
  onError: (error, variables) => {
    selected.value = variables.active
      ? selected.value.filter((item) => item !== variables.code)
      : [...selected.value, variables.code]
    errorMessage.value = normalizeHttpError(error).message
  },
})

function isSelected(code: ReactionRequestReactionCode): boolean {
  return selected.value.includes(code)
}

function toggle(code: ReactionRequestReactionCode): void {
  if (isOwnPublication.value || mutation.isPending.value) return
  if (atLimit.value && !isSelected(code)) {
    // Toque que não faz nada é o pior retorno para quem tem pouca familiaridade
    // com tecnologia — sempre explicar, nunca ignorar (impl10.md v10 §15.3).
    limitMessage.value = 'Você já escolheu 3. Tire uma para poder escolher outra.'
    return
  }
  limitMessage.value = null
  errorMessage.value = null
  const active = !isSelected(code)
  selected.value = active
    ? [...selected.value, code]
    : selected.value.filter((item) => item !== code)
  mutation.mutate({ code, active })
}

function openPicker(): void {
  if (isOwnPublication.value) return
  if (!authStore.authenticated) {
    showAuthNotice()
    return
  }
  limitMessage.value = null
  pickerOpen.value = true
}
</script>

<template>
  <div class="reaction-bar" aria-label="Reações">
    <span
      v-for="reaction in usedReactions"
      :key="reaction.code"
      class="reaction-bar__badge"
      :class="{ 'reaction-bar__badge--selected': isSelected(reaction.code) }"
    >
      <AppIcon v-if="isSelected(reaction.code)" name="check" :size="13" :stroke-width="2.4" />
      {{ reaction.label }}
    </span>
    <button type="button" class="reaction-bar__reagir" :disabled="isOwnPublication" @click="openPicker">
      <AppIcon name="react" :size="17" :stroke-width="1.8" />
      Reagir
    </button>
    <span v-if="isOwnPublication" class="reaction-bar__hint">
      Você não pode reagir à própria publicação.
    </span>
    <span v-if="errorMessage" class="reaction-bar__error" role="alert">{{ errorMessage }}</span>

    <BaseDialog v-model:open="pickerOpen" title="Como você reagiu?" description="Escolha até 3. Toque de novo para tirar.">
      <p class="reaction-bar__picker-count">{{ selected.length }} de 3 escolhidas</p>
      <div class="reaction-bar__picker-grid">
        <button
          v-for="reaction in REACTIONS"
          :key="reaction.code"
          type="button"
          class="reaction-bar__button"
          :class="{ 'reaction-bar__button--selected': isSelected(reaction.code) }"
          :aria-pressed="isSelected(reaction.code)"
          :disabled="mutation.isPending.value"
          @click="toggle(reaction.code)"
        >
          {{ reaction.label }}
        </button>
      </div>
      <span v-if="limitMessage" class="reaction-bar__hint" role="alert">{{ limitMessage }}</span>

      <template #actions>
        <BaseButton @click="pickerOpen = false">Pronto</BaseButton>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.reaction-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-6) var(--space-5);
  border-top: 1px solid var(--color-border);
}

.reaction-bar__badge {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.reaction-bar__badge--selected {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  border-color: var(--color-primary);
}

.reaction-bar__reagir {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  border-style: dashed;
}

.reaction-bar__reagir:disabled {
  opacity: 0.6;
}

.reaction-bar__button {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.reaction-bar__button:hover:not(:disabled),
.reaction-bar__button--selected {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.reaction-bar__button--selected {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
}

.reaction-bar__button:disabled {
  opacity: 0.6;
}

.reaction-bar__error,
.reaction-bar__hint {
  flex-basis: 100%;
  font-size: var(--font-size-xs);
}

.reaction-bar__error {
  color: var(--color-danger);
}

.reaction-bar__hint {
  color: var(--color-text-secondary);
}

.reaction-bar__picker-count {
  margin: 0 0 var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.reaction-bar__picker-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

@media (max-width: 30rem) {
  .reaction-bar {
    padding-inline: var(--space-4);
  }
}
</style>
