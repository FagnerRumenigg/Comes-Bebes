<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { getFindByUsernameQueryKey, useFindByUsername } from '@/api/generated/profiles/profiles'
import { useBlock, useGetUserPublications } from '@/api/generated/users/users'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const username = computed(() => String(route.params.username ?? ''))
const profileQuery = useFindByUsername(username)
const userId = computed(() => profileQuery.data.value?.id ?? '')
const publicationsQuery = useGetUserPublications(userId, { page: 1, size: 20 })

const blockDialogOpen = ref(false)
const blockReason = ref('')
const blockError = ref('')

const canBlock = computed(
  () =>
    authStore.isAdmin &&
    Boolean(profileQuery.data.value) &&
    profileQuery.data.value?.id !== authStore.identity?.userId &&
    profileQuery.data.value?.status !== 'BLOCKED',
)

const blockMutation = useBlock({
  mutation: {
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: getFindByUsernameQueryKey(username) })
      blockDialogOpen.value = false
      blockReason.value = ''
    },
    onError: (error) => {
      blockError.value = normalizeHttpError(error).message
    },
  },
})

function confirmBlock(): void {
  if (blockMutation.isPending.value) return
  blockError.value = ''
  if (!blockReason.value.trim()) {
    blockError.value = 'Informe o motivo do bloqueio.'
    return
  }
  blockMutation.mutate({ id: userId.value, data: { reason: blockReason.value.trim() } })
}
</script>

<template>
  <section class="profile-view">
    <div v-if="profileQuery.isPending.value" class="profile-view__state">Carregando perfil...</div>
    <div v-else-if="profileQuery.isError.value" class="profile-view__state" role="alert">
      Perfil não encontrado.
    </div>
    <template v-else-if="profileQuery.data.value">
      <header class="profile-view__header">
        <p class="profile-view__eyebrow">Perfil público</p>
        <h1>{{ profileQuery.data.value.displayName }}</h1>
        <p class="profile-view__username">@{{ profileQuery.data.value.username }}</p>
        <BaseButton
          v-if="canBlock"
          variant="ghost"
          class="profile-view__block"
          @click="blockDialogOpen = true"
        >
          Bloquear usuário
        </BaseButton>
      </header>

      <BaseDialog
        v-model:open="blockDialogOpen"
        title="Bloquear usuário"
        description="A conta será anonimizada e não poderá mais autenticar, publicar, reagir, salvar ou denunciar."
      >
        <BaseTextarea
          v-model="blockReason"
          label="Motivo do bloqueio"
          required
          maxlength="2000"
          :rows="4"
        />
        <BaseFieldError v-if="blockError" :message="blockError" />
        <div class="profile-view__dialog-actions">
          <BaseButton variant="ghost" @click="blockDialogOpen = false">Cancelar</BaseButton>
          <BaseButton :loading="blockMutation.isPending.value" @click="confirmBlock">
            Bloquear
          </BaseButton>
        </div>
      </BaseDialog>
      <div v-if="publicationsQuery.isPending.value" class="profile-view__state">
        Carregando publicações...
      </div>
      <div v-else-if="!publicationsQuery.data.value?.content.length" class="profile-view__state">
        Este perfil ainda não tem publicações visíveis.
      </div>
      <div v-else class="profile-view__list">
        <PublicationCard
          v-for="publication in publicationsQuery.data.value.content"
          :key="publication.id"
          :publication="publication"
        />
      </div>
    </template>
  </section>
</template>

<style scoped>
.profile-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.profile-view__header {
  margin-block-end: var(--space-10);
}
.profile-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.profile-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.profile-view__username {
  margin-block-start: var(--space-2);
  color: var(--color-text-secondary);
}

.profile-view__block {
  margin-block-start: var(--space-4);
  color: var(--color-danger);
}

.profile-view__dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-block-start: var(--space-4);
}
.profile-view__list {
  display: grid;
  gap: var(--space-8);
}
.profile-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
