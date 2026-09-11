<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'

import { useDeletePublication } from '@/api/generated/publications/publications'
import type { PublicationResponse } from '@/api/generated/models'
import AppIcon from '@/components/icons/AppIcon.vue'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

import PublicationHeader from './PublicationHeader.vue'
import PublicationImage from './PublicationImage.vue'
import ReactionBar from './ReactionBar.vue'
import RecipeFlipCard from './RecipeFlipCard.vue'
import ReportDialog from './ReportDialog.vue'
import SaveButton from './SaveButton.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'

const props = defineProps<{
  publication: PublicationResponse
}>()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const deleteDialogOpen = ref(false)
const deleteError = ref('')

const typeLabel = computed(() => {
  const labels: Record<PublicationResponse['type'], string> = {
    DISH: 'Prato',
    RECIPE: 'Receita',
    MY_VERSION: 'Minha versão',
  }
  return labels[props.publication.type]
})

const statusMessage = computed(() => {
  const messages: Partial<Record<PublicationResponse['status'], string>> = {
    PENDING_VALIDATION: 'A imagem está sendo validada antes de aparecer no feed.',
    UNDER_REVIEW: 'Esta publicação está em análise pela moderação.',
    HIDDEN: 'Esta publicação foi ocultada.',
    REJECTED: 'Esta publicação não foi aprovada.',
    REMOVED: 'Esta publicação foi removida.',
  }
  return messages[props.publication.status] ?? null
})

const imageAlt = computed(
  () => props.publication.title ?? `Publicação de ${props.publication.authorDisplayName}`,
)

const deleteMutation = useDeletePublication({
  mutation: {
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['publications'] })
      deleteDialogOpen.value = false
    },
    onError: (error) => {
      deleteError.value = normalizeHttpError(error).message
    },
  },
})

function confirmDelete(): void {
  if (deleteMutation.isPending.value) return
  deleteError.value = ''
  deleteMutation.mutate({ id: props.publication.id })
}
</script>

<template>
  <article class="publication-card">
    <PublicationHeader
      :publication-id="publication.id"
      :author-id="publication.authorId"
      :author-display-name="publication.authorDisplayName"
      :author-username="publication.authorUsername"
      :author-avatar-key="publication.authorAvatarKey"
      :published-at="publication.publishedAt"
      :photo-taken-at="publication.photoTakenAt"
      :visibility="publication.visibility"
    />

    <div v-if="statusMessage" class="publication-card__status" role="status">
      <span class="publication-card__status-label">{{ typeLabel }}</span>
      <strong>{{ statusMessage }}</strong>
    </div>
    <div v-else class="publication-card__media">
      <span
        v-if="publication.type === 'RECIPE' || publication.type === 'MY_VERSION'"
        class="publication-card__tag"
      >
        Receita
      </span>
      <RecipeFlipCard v-if="publication.recipePreview" :publication="publication" />
      <PublicationImage v-else :src="publication.imageUrl" :alt="imageAlt" />
    </div>

    <div class="publication-card__body">
      <span class="publication-card__type">{{ typeLabel }}</span>
      <h2 v-if="publication.title">{{ publication.title }}</h2>
      <p v-if="publication.description">{{ publication.description }}</p>
    </div>
    <div class="publication-card__actions">
      <ReactionBar :publication="publication" />
      <SaveButton :publication-id="publication.id" :saved="publication.saved" />
      <ReportDialog
        :publication-id="publication.id"
        :author-id="publication.authorId"
        :reported="publication.reportedByCurrentUser"
      />
      <RouterLink
        v-if="
          authStore.authenticated &&
          (publication.type === 'RECIPE' || publication.type === 'MY_VERSION')
        "
        class="publication-card__versions"
        :to="`/publicar/minha-versao/${publication.id}`"
      >
        <AppIcon name="my-version" :size="18" :stroke-width="1.9" />
        Minha versão
      </RouterLink>
      <button
        v-else-if="publication.type === 'RECIPE' || publication.type === 'MY_VERSION'"
        type="button"
        class="publication-card__versions"
        @click="showAuthNotice"
      >
        <AppIcon name="my-version" :size="18" :stroke-width="1.9" />
        Minha versão
      </button>
      <div v-if="authStore.identity?.userId === publication.authorId" class="publication-card__owner-actions">
        <RouterLink
          class="publication-card__edit"
          :to="`/publicacoes/${publication.id}/editar`"
        >
          Editar
        </RouterLink>
        <button
          type="button"
          class="publication-card__delete"
          @click="deleteDialogOpen = true"
        >
          Excluir
        </button>
      </div>
    </div>

    <BaseDialog
      v-model:open="deleteDialogOpen"
      title="Excluir publicação"
      description="Tem certeza que deseja excluir esta publicação? Essa ação não pode ser desfeita."
    >
      <p v-if="deleteError" role="alert" class="publication-card__delete-error">
        {{ deleteError }}
      </p>
      <div class="publication-card__dialog-actions">
        <BaseButton variant="ghost" @click="deleteDialogOpen = false">Cancelar</BaseButton>
        <BaseButton variant="danger" :loading="deleteMutation.isPending.value" @click="confirmDelete">
          Excluir
        </BaseButton>
      </div>
    </BaseDialog>
  </article>
</template>

<style scoped>
.publication-card {
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.publication-card__media {
  position: relative;
}

.publication-card__tag {
  position: absolute;
  top: var(--space-3);
  left: var(--space-3);
  z-index: 1;
  padding: var(--space-1) var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  background: var(--color-accent);
  border-radius: var(--radius-pill);
}

.publication-card__body {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-6);
}

.publication-card__type,
.publication-card__status-label {
  width: fit-content;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.publication-card h2 {
  margin: 0;
  font-size: var(--font-size-2xl);
}

.publication-card p {
  margin: 0;
  color: var(--color-text-secondary);
}

.publication-card__status {
  display: grid;
  min-height: 16rem;
  place-content: center;
  gap: var(--space-3);
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: color-mix(in srgb, var(--color-border) 28%, var(--color-surface));
}

.publication-card__status-label {
  margin-inline: auto;
}

.publication-card__status strong {
  max-width: 30rem;
  color: var(--color-text);
  font-family: var(--font-editorial);
  font-size: var(--font-size-xl);
}

.publication-card__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-6) var(--space-5);
  border-top: 1px solid var(--color-border);
}

.publication-card__actions :deep(.reaction-bar) {
  flex-basis: 100%;
  padding: 0;
  border: 0;
}

.publication-card__versions,
.publication-card__edit,
.publication-card__delete {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
}

.publication-card__edit {
  text-decoration: none;
}

.publication-card__owner-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--space-4);
  margin-inline-start: auto;
}

.publication-card__delete {
  color: var(--color-danger);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
}

.publication-card__delete-error {
  color: var(--color-danger);
}

.publication-card__dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

@media (max-width: 30rem) {
  .publication-card__body {
    padding: var(--space-5) var(--space-4);
  }

  .publication-card__actions {
    padding-inline: var(--space-4);
  }
}
</style>
