<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import { useUnseen } from '@/api/generated/patch-notes/patch-notes'
import { useMarkPatchNotesSeen } from '@/api/generated/users/users'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const authStore = useAuthStore()
const errorMessage = ref<string | null>(null)

const unseenQuery = useUnseen()

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

const markSeenMutation = useMarkPatchNotesSeen({
  mutation: {
    onSuccess: () => {
      if (authStore.identity) {
        authStore.identity.hasUnseenPatchNotes = false
      }
      void router.replace({ name: 'feed' })
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
</script>

<template>
  <section class="patch-notes-view" aria-labelledby="patch-notes-title">
    <div class="patch-notes-view__content">
      <p class="patch-notes-view__eyebrow">Novidades</p>
      <h1 id="patch-notes-title">O que mudou desde sua última visita</h1>

      <div v-if="unseenQuery.isPending.value" class="patch-notes-view__state" aria-label="Carregando novidades">
        <div class="patch-notes-view__skeleton" />
      </div>

      <div v-else-if="unseenQuery.isError.value" class="patch-notes-view__state" role="alert">
        <p>{{ normalizeHttpError(unseenQuery.error.value).message }}</p>
        <BaseButton variant="secondary" @click="unseenQuery.refetch()">Tentar novamente</BaseButton>
      </div>

      <ul v-else class="patch-notes-view__list">
        <li v-for="note in unseenQuery.data.value" :key="note.id" class="patch-notes-view__note">
          <span class="patch-notes-view__date">{{ dateFormatter.format(new Date(note.publishedAt)) }}</span>
          <h2>{{ note.title }}</h2>
          <p>{{ note.body }}</p>
        </li>
      </ul>

      <BaseButton
        class="patch-notes-view__action"
        :loading="markSeenMutation.isPending.value"
        @click="confirm"
      >
        Entendi
      </BaseButton>

      <p v-if="errorMessage" class="patch-notes-view__error" role="alert">{{ errorMessage }}</p>
    </div>
  </section>
</template>

<style scoped>
.patch-notes-view {
  display: grid;
  place-items: center;
  min-height: 70vh;
}

.patch-notes-view__content {
  width: min(100%, 42rem);
  display: grid;
  gap: var(--space-5);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.patch-notes-view__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.patch-notes-view h1 {
  margin: 0;
}

.patch-notes-view__list {
  display: grid;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

.patch-notes-view__note {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface) 70%, var(--color-background));
}

.patch-notes-view__note h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.patch-notes-view__note p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.patch-notes-view__date {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.patch-notes-view__state {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
}

.patch-notes-view__skeleton {
  min-height: 8rem;
  background: linear-gradient(
    100deg,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 30%,
    color-mix(in srgb, var(--color-border) 58%, var(--color-surface)) 50%,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 70%
  );
  background-size: 200% 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  animation: patch-notes-skeleton var(--duration-slow) linear infinite;
}

.patch-notes-view__action {
  justify-self: start;
}

.patch-notes-view__error {
  color: var(--color-danger);
}

@keyframes patch-notes-skeleton {
  to {
    background-position: -200% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .patch-notes-view__skeleton {
    animation: none;
  }
}
</style>
