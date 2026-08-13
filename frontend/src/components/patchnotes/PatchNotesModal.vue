<script setup lang="ts">
import { computed, ref } from 'vue'

import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import { useUnseen } from '@/api/generated/patch-notes/patch-notes'
import { useMarkPatchNotesSeen } from '@/api/generated/users/users'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const authStore = useAuthStore()
const errorMessage = ref<string | null>(null)

const shouldShow = computed(
  () =>
    authStore.authenticated &&
    authStore.identity?.onboardingCompleted !== false &&
    authStore.identity?.hasUnseenPatchNotes === true,
)

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

const unseenQuery = useUnseen({ query: { enabled: shouldShow } })

const markSeenMutation = useMarkPatchNotesSeen({
  mutation: {
    onSuccess: () => {
      if (authStore.identity) authStore.identity.hasUnseenPatchNotes = false
    },
    onError: (error) => {
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

function confirm(): void {
  if (!authStore.identity?.userId || markSeenMutation.isPending.value) return
  errorMessage.value = null
  void markSeenMutation.mutateAsync({ id: authStore.identity.userId })
}

function onDismiss(open: boolean): void {
  if (!open) confirm()
}
</script>

<template>
  <BaseDialog
    :open="shouldShow"
    title="Novidades"
    description="O que mudou desde sua última visita"
    close-label="Fechar novidades"
    @update:open="onDismiss"
  >
    <div
      v-if="unseenQuery.isPending.value"
      class="patch-notes-modal__state"
      aria-label="Carregando novidades"
    >
      <div class="patch-notes-modal__skeleton" />
    </div>

    <div v-else-if="unseenQuery.isError.value" class="patch-notes-modal__state" role="alert">
      <p>{{ normalizeHttpError(unseenQuery.error.value).message }}</p>
      <BaseButton variant="secondary" @click="unseenQuery.refetch()">Tentar novamente</BaseButton>
    </div>

    <ul v-else class="patch-notes-modal__list">
      <li v-for="note in unseenQuery.data.value" :key="note.id" class="patch-notes-modal__note">
        <span class="patch-notes-modal__date">{{
          dateFormatter.format(new Date(note.publishedAt))
        }}</span>
        <h3>{{ note.title }}</h3>
        <p>{{ note.body }}</p>
      </li>
    </ul>

    <p v-if="errorMessage" class="patch-notes-modal__error" role="alert">{{ errorMessage }}</p>

    <div class="patch-notes-modal__actions">
      <BaseButton :loading="markSeenMutation.isPending.value" @click="confirm">Entendi</BaseButton>
    </div>
  </BaseDialog>
</template>

<style scoped>
.patch-notes-modal__list {
  display: grid;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

.patch-notes-modal__note {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface) 70%, var(--color-background));
}

.patch-notes-modal__note h3 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.patch-notes-modal__note p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.patch-notes-modal__date {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.patch-notes-modal__state {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.patch-notes-modal__skeleton {
  min-height: 8rem;
  width: 100%;
  background: linear-gradient(
    100deg,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 30%,
    color-mix(in srgb, var(--color-border) 58%, var(--color-surface)) 50%,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 70%
  );
  background-size: 200% 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  animation: patch-notes-modal-skeleton var(--duration-slow) linear infinite;
}

.patch-notes-modal__error {
  color: var(--color-danger);
}

.patch-notes-modal__actions {
  display: flex;
  justify-content: flex-end;
  margin-block-start: var(--space-2);
}

@keyframes patch-notes-modal-skeleton {
  to {
    background-position: -200% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .patch-notes-modal__skeleton {
    animation: none;
  }
}
</style>
