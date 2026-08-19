<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import {
  useDeletePublication,
  useGetPublicationById,
} from '@/api/generated/publications/publications'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import IngredientList from '@/components/recipe/IngredientList.vue'
import OriginReference from '@/components/recipe/OriginReference.vue'
import RecipeInstructions from '@/components/recipe/RecipeInstructions.vue'
import PublicationImage from '@/components/publication/PublicationImage.vue'
import ReactionBar from '@/components/publication/ReactionBar.vue'
import ReportDialog from '@/components/publication/ReportDialog.vue'
import SaveButton from '@/components/publication/SaveButton.vue'
import { normalizeHttpError } from '@/api/errors'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const publicationId = computed(() => String(route.params.id))
const publicationQuery = useGetPublicationById(publicationId)
const myVersionOpen = ref(false)
const deleteDialogOpen = ref(false)
const deleteError = ref('')

const errorMessage = computed(() => normalizeHttpError(publicationQuery.error.value).message)

const recipe = computed(() => publicationQuery.data.value?.recipePreview)
const isOwner = computed(() => authStore.identity?.userId === publicationQuery.data.value?.authorId)
const canManage = computed(() => isOwner.value || authStore.isAdmin)
const typeLabel = computed(() => {
  const type = publicationQuery.data.value?.type
  return type === 'MY_VERSION' ? 'Minha versão' : type === 'RECIPE' ? 'Receita' : 'Prato'
})

const deleteMutation = useDeletePublication({
  mutation: {
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['publications'] })
      deleteDialogOpen.value = false
      void router.push('/')
    },
    onError: (error) => {
      deleteError.value = normalizeHttpError(error).message
    },
  },
})

function goBack(): void {
  const state = window.history.state as { back?: string | null } | null
  if (state?.back) {
    router.back()
    return
  }
  void router.push('/')
}

function startMyVersion(): void {
  if (!authStore.authenticated) {
    showAuthNotice()
    return
  }
  myVersionOpen.value = true
}

function confirmDelete(): void {
  if (deleteMutation.isPending.value) return
  deleteError.value = ''
  deleteMutation.mutate({ id: publicationId.value })
}
</script>

<template>
  <article class="publication-details">
    <button type="button" class="publication-details__back" @click="goBack">
      <span aria-hidden="true">←</span> Voltar
    </button>
    <div v-if="publicationQuery.isPending.value" class="publication-details__state">
      Carregando publicação…
    </div>
    <div v-else-if="publicationQuery.isError.value" class="publication-details__state" role="alert">
      <strong>Não foi possível abrir esta publicação.</strong>
      <p>{{ errorMessage }}</p>
      <BaseButton @click="publicationQuery.refetch()">Tentar novamente</BaseButton>
    </div>
    <template v-else-if="publicationQuery.data.value">
      <header class="publication-details__header">
        <span>{{ typeLabel }}</span>
        <h1>{{ publicationQuery.data.value.title ?? 'Publicação da comunidade' }}</h1>
        <p v-if="publicationQuery.data.value.editedByAdmin" class="publication-details__admin-badge">
          Editada por um administrador
        </p>
        <p>
          Por
          <RouterLink :to="`/u/${publicationQuery.data.value.authorUsername}`">
            {{ publicationQuery.data.value.authorDisplayName }}
          </RouterLink>
          · @{{ publicationQuery.data.value.authorUsername }}
        </p>
        <OriginReference
          v-if="publicationQuery.data.value.originalPublicationId"
          :source-id="publicationQuery.data.value.originalPublicationId"
        />
      </header>

      <PublicationImage
        :src="publicationQuery.data.value.imageUrl"
        :alt="publicationQuery.data.value.title ?? 'Imagem da publicação'"
      />

      <div class="publication-details__actions">
        <RouterLink
          v-if="canManage"
          class="publication-details__edit"
          :to="`/publicacoes/${publicationQuery.data.value.id}/editar`"
        >
          Editar publicação
        </RouterLink>
        <BaseButton
          v-if="canManage"
          variant="ghost"
          class="publication-details__delete"
          @click="deleteDialogOpen = true"
        >
          Excluir publicação
        </BaseButton>
        <ReactionBar :publication="publicationQuery.data.value" />
        <SaveButton
          :publication-id="publicationQuery.data.value.id"
          :saved="publicationQuery.data.value.saved"
        />
        <ReportDialog
          :publication-id="publicationQuery.data.value.id"
          :author-id="publicationQuery.data.value.authorId"
          :reported="publicationQuery.data.value.reportedByCurrentUser"
        />
        <BaseButton
          v-if="
            publicationQuery.data.value.type === 'RECIPE' ||
            publicationQuery.data.value.type === 'MY_VERSION'
          "
          variant="secondary"
          @click="startMyVersion"
        >
          Publicar minha versão ({{ publicationQuery.data.value.versionsCount }})
        </BaseButton>
      </div>

      <p v-if="publicationQuery.data.value.description" class="publication-details__description">
        {{ publicationQuery.data.value.description }}
      </p>

      <ul v-if="publicationQuery.data.value.tags.length" class="publication-details__tags">
        <li v-for="tag in publicationQuery.data.value.tags" :key="tag.slug">{{ tag.name }}</li>
      </ul>

      <template v-if="recipe">
        <div class="publication-details__yield">
          <span>Rendimento</span>
          <strong> {{ recipe.yieldQuantity ?? 'A gosto' }} {{ recipe.yieldUnit ?? '' }} </strong>
        </div>
        <IngredientList :ingredients="recipe.ingredients" />
        <RecipeInstructions :instructions="recipe.instructions" />
      </template>

      <BaseDialog
        v-model:open="myVersionOpen"
        title="Publicar minha versão"
        description="Começamos com os ingredientes e o preparo da receita original — você ajusta à vontade e envia uma nova foto para mostrar como ficou a sua versão."
      >
        <div class="publication-details__dialog-actions">
          <BaseButton variant="ghost" @click="myVersionOpen = false">Cancelar</BaseButton>
          <BaseButton @click="myVersionOpen = false">
            <RouterLink
              class="publication-details__continue"
              :to="`/publicar/minha-versao/${publicationQuery.data.value.id}`"
            >
              Cadastrar minha versão
            </RouterLink>
          </BaseButton>
        </div>
      </BaseDialog>

      <BaseDialog
        v-model:open="deleteDialogOpen"
        title="Excluir publicação"
        description="Tem certeza que deseja excluir esta publicação? Essa ação não pode ser desfeita."
      >
        <p v-if="deleteError" role="alert" class="publication-details__delete-error">
          {{ deleteError }}
        </p>
        <div class="publication-details__dialog-actions">
          <BaseButton variant="ghost" @click="deleteDialogOpen = false">Cancelar</BaseButton>
          <BaseButton :loading="deleteMutation.isPending.value" @click="confirmDelete">
            Excluir
          </BaseButton>
        </div>
      </BaseDialog>
    </template>
  </article>
</template>

<style scoped>
.publication-details {
  width: min(100%, var(--content-feed));
  margin-inline: auto;
}

.publication-details__back {
  display: inline-flex;
  min-height: var(--control-min-size);
  align-items: center;
  gap: var(--space-2);
  margin-block-end: var(--space-5);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
}

.publication-details__back:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.publication-details__header {
  display: grid;
  gap: var(--space-3);
  margin-block-end: var(--space-8);
}

.publication-details__header > span {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.publication-details__header h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
}

.publication-details__header p,
.publication-details__description {
  margin: 0;
  color: var(--color-text-secondary);
}

.publication-details__header a {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.publication-details :deep(.publication-image) {
  aspect-ratio: 4 / 5;
}

.publication-details__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  padding-block: var(--space-5);
  border-block-end: 1px solid var(--color-border);
}

.publication-details__actions :deep(.reaction-bar) {
  flex-basis: 100%;
  padding: 0;
  border: 0;
}

.publication-details__edit {
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
}

.publication-details__delete {
  color: var(--color-danger);
}

.publication-details__admin-badge {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-style: italic;
}

.publication-details__delete-error {
  margin: 0;
  color: var(--color-danger);
}

.publication-details__description {
  padding-block: var(--space-8);
  font-size: var(--font-size-lg);
  line-height: var(--line-height-body);
}

.publication-details__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: 0 0 var(--space-6);
  padding: 0;
  list-style: none;
}

.publication-details__tags li {
  padding: var(--space-2) var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.publication-details__yield {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding-block: var(--space-5);
  border-block: 1px solid var(--color-border);
}

.publication-details__yield span {
  color: var(--color-text-secondary);
}

.publication-details__state {
  display: grid;
  place-items: center;
  gap: var(--space-3);
  min-height: 24rem;
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.publication-details__state p {
  margin: 0;
}

.publication-details__dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
}

.publication-details__continue {
  color: inherit;
  text-decoration: none;
}
</style>
