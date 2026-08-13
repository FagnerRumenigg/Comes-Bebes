<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import BaseDialog from '@/components/base/BaseDialog.vue'
import { applyReaction, removeReaction } from '@/api/generated/publications/publications'
import type { PublicationResponse, ReactionRequestReactionCode } from '@/api/generated/models'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{ publication: PublicationResponse }>()

const MAX_REACTIONS = 3

const PRIMARY_REACTIONS: Array<{ code: ReactionRequestReactionCode; label: string }> = [
  { code: 'WOULD_EAT', label: 'Eu comeria' },
  { code: 'WANT_TO_MAKE', label: 'Quero fazer' },
  { code: 'COMFORT_FOOD', label: 'Comida afetiva' },
]

const SECONDARY_REACTIONS: Array<{ code: ReactionRequestReactionCode; label: string }> = [
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
const moreOpen = ref(false)
const isOwnPublication = computed(() => authStore.identity?.userId === props.publication.authorId)
const atLimit = computed(() => selected.value.length >= MAX_REACTIONS)
const activeSecondary = computed(() =>
  SECONDARY_REACTIONS.filter((reaction) => selected.value.includes(reaction.code)),
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

const total = (code: ReactionRequestReactionCode): number =>
  props.publication.reactionTotals?.[code] ?? 0

function isSelected(code: ReactionRequestReactionCode): boolean {
  return selected.value.includes(code)
}

function isDisabled(code: ReactionRequestReactionCode): boolean {
  return mutation.isPending.value || (atLimit.value && !isSelected(code))
}

function toggle(code: ReactionRequestReactionCode): void {
  if (isOwnPublication.value || isDisabled(code)) return
  errorMessage.value = null
  const active = !isSelected(code)
  selected.value = active
    ? [...selected.value, code]
    : selected.value.filter((item) => item !== code)
  mutation.mutate({ code, active })
}

function openMore(): void {
  if (isOwnPublication.value) return
  moreOpen.value = true
}
</script>

<template>
  <div class="reaction-bar" aria-label="Reações">
    <template v-if="authStore.authenticated">
      <button
        v-for="reaction in [...PRIMARY_REACTIONS, ...activeSecondary]"
        :key="reaction.code"
        type="button"
        class="reaction-bar__button"
        :class="{ 'reaction-bar__button--selected': isSelected(reaction.code) }"
        :aria-pressed="isSelected(reaction.code)"
        :disabled="isOwnPublication || isDisabled(reaction.code)"
        @click="toggle(reaction.code)"
      >
        <span>{{ reaction.label }}</span>
        <span v-if="publication.showReactionCounts" class="reaction-bar__count">
          {{ total(reaction.code) }}
        </span>
      </button>
      <button
        type="button"
        class="reaction-bar__button reaction-bar__more"
        :disabled="isOwnPublication"
        @click="openMore"
      >
        + Reagir
      </button>
    </template>
    <template v-else>
      <button
        v-for="reaction in PRIMARY_REACTIONS"
        :key="reaction.code"
        type="button"
        class="reaction-bar__button"
        @click="showAuthNotice"
      >
        <span>{{ reaction.label }}</span>
        <span v-if="publication.showReactionCounts" class="reaction-bar__count">
          {{ total(reaction.code) }}
        </span>
      </button>
      <button type="button" class="reaction-bar__button reaction-bar__more" @click="showAuthNotice">
        + Reagir
      </button>
    </template>
    <span v-if="isOwnPublication" class="reaction-bar__hint">
      Você não pode reagir à própria publicação.
    </span>
    <span v-else-if="atLimit && authStore.authenticated" class="reaction-bar__hint">
      Você pode escolher até 3 reações para esta publicação.
    </span>
    <span v-if="errorMessage" class="reaction-bar__error" role="alert">{{ errorMessage }}</span>

    <BaseDialog
      v-model:open="moreOpen"
      title="Mais reações"
      description="Escolha até 3 reações no total para esta publicação."
    >
      <div class="reaction-bar__more-grid">
        <button
          v-for="reaction in SECONDARY_REACTIONS"
          :key="reaction.code"
          type="button"
          class="reaction-bar__button"
          :class="{ 'reaction-bar__button--selected': isSelected(reaction.code) }"
          :aria-pressed="isSelected(reaction.code)"
          :disabled="isDisabled(reaction.code)"
          @click="toggle(reaction.code)"
        >
          <span>{{ reaction.label }}</span>
          <span v-if="publication.showReactionCounts" class="reaction-bar__count">
            {{ total(reaction.code) }}
          </span>
        </button>
      </div>
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

.reaction-bar__button {
  display: inline-flex;
  min-height: 2.25rem;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  text-decoration: none;
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

.reaction-bar__more {
  font-weight: var(--font-weight-semibold);
  border-style: dashed;
}

.reaction-bar__count {
  font-weight: var(--font-weight-bold);
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

.reaction-bar__more-grid {
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
