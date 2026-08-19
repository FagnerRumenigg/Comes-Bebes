<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import {
  getGetCollectionByIdQueryKey,
  getGetCollectionPublicationsQueryKey,
  useDeleteCollection,
  useGetCollectionById,
  useGetCollectionPublications,
  useRemoveCollectionPublication,
} from '@/api/generated/collections/collections'
import type { CollectionResponse } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import CollectionFollowButton from '@/components/collection/CollectionFollowButton.vue'
import CollectionFormDialog from '@/components/collection/CollectionFormDialog.vue'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()

const collectionId = computed(() => String(route.params.id ?? ''))
const collectionQuery = useGetCollectionById(collectionId)
const publicationsQuery = useGetCollectionPublications(collectionId, { page: 1, size: 20 })

const isOwner = computed(
  () =>
    Boolean(collectionQuery.data.value) &&
    collectionQuery.data.value?.authorId === authStore.identity?.userId,
)

const editOpen = ref(false)
const deleteError = ref('')
const linkCopied = ref(false)

function refreshCollection(): void {
  void queryClient.invalidateQueries({ queryKey: getGetCollectionByIdQueryKey(collectionId) })
}

function handleSaved(collection: CollectionResponse): void {
  queryClient.setQueryData(getGetCollectionByIdQueryKey(collectionId), collection)
}

const deleteMutation = useDeleteCollection({
  mutation: {
    onSuccess: async () => {
      await router.push(`/u/${authStore.identity?.username}`)
    },
    onError: (error) => {
      deleteError.value = normalizeHttpError(error).message
    },
  },
})

function confirmDelete(): void {
  if (deleteMutation.isPending.value) return
  if (!window.confirm('Excluir esta coleção? Essa ação não pode ser desfeita.')) return
  deleteError.value = ''
  deleteMutation.mutate({ id: collectionId.value })
}

const removeMutation = useRemoveCollectionPublication({
  mutation: {
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: getGetCollectionPublicationsQueryKey(collectionId),
      })
      refreshCollection()
    },
  },
})

function removePublication(publicationId: string): void {
  removeMutation.mutate({ id: collectionId.value, publicationId })
}

async function copyLink(): Promise<void> {
  try {
    await navigator.clipboard.writeText(window.location.href)
    linkCopied.value = true
    setTimeout(() => {
      linkCopied.value = false
    }, 2000)
  } catch {
    // Sem clipboard disponível, o usuário ainda pode copiar a URL manualmente.
  }
}
</script>

<template>
  <section class="collection-details-view">
    <div v-if="collectionQuery.isPending.value" class="collection-details-view__state">
      Carregando coleção...
    </div>
    <div v-else-if="collectionQuery.isError.value" class="collection-details-view__state" role="alert">
      Coleção não encontrada.
    </div>
    <template v-else-if="collectionQuery.data.value">
      <header class="collection-details-view__header">
        <p class="collection-details-view__eyebrow">
          Coleção de
          <RouterLink :to="`/u/${collectionQuery.data.value.authorUsername}`">
            @{{ collectionQuery.data.value.authorUsername }}
          </RouterLink>
          <span v-if="collectionQuery.data.value.visibility === 'PRIVATE'"> · Privada</span>
        </p>
        <h1>{{ collectionQuery.data.value.name }}</h1>
        <p v-if="collectionQuery.data.value.description">
          {{ collectionQuery.data.value.description }}
        </p>
        <p class="collection-details-view__counts">
          <strong>{{ collectionQuery.data.value.publicationsCount }}</strong>
          {{ collectionQuery.data.value.publicationsCount === 1 ? 'publicação' : 'publicações' }} ·
          <strong>{{ collectionQuery.data.value.followersCount }}</strong>
          {{ collectionQuery.data.value.followersCount === 1 ? 'seguidor' : 'seguidores' }}
        </p>
        <div class="collection-details-view__actions">
          <CollectionFollowButton
            v-if="!isOwner"
            :collection-id="collectionQuery.data.value.id"
            :following="Boolean(collectionQuery.data.value.followedByCurrentUser)"
            @toggled="refreshCollection"
          />
          <template v-if="isOwner">
            <BaseButton variant="secondary" @click="editOpen = true">Editar</BaseButton>
            <BaseButton
              variant="danger"
              :loading="deleteMutation.isPending.value"
              @click="confirmDelete"
            >
              Excluir
            </BaseButton>
          </template>
          <BaseButton variant="ghost" @click="copyLink">
            {{ linkCopied ? 'Link copiado!' : 'Compartilhar link' }}
          </BaseButton>
        </div>
        <p v-if="deleteError" class="collection-details-view__error" role="alert">
          {{ deleteError }}
        </p>
      </header>

      <CollectionFormDialog
        v-model:open="editOpen"
        :collection="collectionQuery.data.value"
        @saved="handleSaved"
      />

      <div v-if="publicationsQuery.isPending.value" class="collection-details-view__state">
        Carregando publicações...
      </div>
      <div
        v-else-if="!publicationsQuery.data.value?.content.length"
        class="collection-details-view__state"
      >
        Esta coleção ainda não tem publicações.
      </div>
      <div v-else class="collection-details-view__list">
        <div
          v-for="publication in publicationsQuery.data.value.content"
          :key="publication.id"
          class="collection-details-view__item"
        >
          <PublicationCard :publication="publication" />
          <button
            v-if="isOwner"
            type="button"
            class="collection-details-view__remove"
            :disabled="removeMutation.isPending.value"
            @click="removePublication(publication.id)"
          >
            Remover da coleção
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.collection-details-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.collection-details-view__header {
  margin-block-end: var(--space-10);
}
.collection-details-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.collection-details-view__eyebrow a {
  color: inherit;
}
.collection-details-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.collection-details-view__header > p {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.collection-details-view__counts strong {
  color: var(--color-text);
}
.collection-details-view__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-block-start: var(--space-4);
}
.collection-details-view__error {
  margin-block-start: var(--space-3);
  color: var(--color-danger);
}
.collection-details-view__list {
  display: grid;
  gap: var(--space-4);
}
.collection-details-view__item {
  display: grid;
  gap: var(--space-2);
}
.collection-details-view__remove {
  justify-self: start;
  padding: 0;
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 0;
}
.collection-details-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
