<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import {
  getCollectionInviteLink,
  getGetCollectionByIdQueryKey,
  getGetCollectionInviteesQueryKey,
  getGetCollectionPublicationsQueryKey,
  regenerateCollectionInviteLink,
  useDeleteCollection,
  useGetCollectionById,
  useGetCollectionInvitees,
  useGetCollectionPublications,
  useRemoveCollectionPublication,
  useUpdateCollection,
} from '@/api/generated/collections/collections'
import type { CollectionResponse, UserResponse } from '@/api/generated/models'
import { apiRequest } from '@/api/client'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import RadioCardGroup from '@/components/base/RadioCardGroup.vue'
import CollectionFollowButton from '@/components/collection/CollectionFollowButton.vue'
import CollectionFormDialog from '@/components/collection/CollectionFormDialog.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import PublicationTile from '@/components/publication/PublicationTile.vue'
import { normalizeHttpError } from '@/api/errors'
import { resolveImageUrl } from '@/utils/resolveImageUrl'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()

// coverImageUrls ainda não passou pelo orval (mesma situação do
// CollectionCard.vue) — trocar por CollectionResponse puro depois de
// rodar o backend e `npm run api:generate`.
type CollectionWithCover = CollectionResponse & { coverImageUrls?: string[] }

const collectionId = computed(() => String(route.params.id ?? ''))
const collectionQuery = useGetCollectionById(collectionId)
const publicationsQuery = useGetCollectionPublications(collectionId, { page: 1, size: 30 })
// Sempre o último prato adicionado à coleção — não é mais um mosaico de várias fotos.
const coverImageUrl = computed(
  () => (collectionQuery.data.value as CollectionWithCover | undefined)?.coverImageUrls?.[0] ?? null,
)

const isOwner = computed(
  () =>
    Boolean(collectionQuery.data.value) &&
    collectionQuery.data.value?.authorId === authStore.identity?.userId,
)

const VISIBILITY_META = {
  PUBLIC: { label: 'Pública', icon: 'globe' as const },
  SHARED: { label: 'Para quem eu escolher', icon: 'people' as const },
  PRIVATE: { label: 'Só para mim', icon: 'lock' as const },
}

const visibilityMeta = computed(() =>
  collectionQuery.data.value ? VISIBILITY_META[collectionQuery.data.value.visibility] : null,
)

const editOpen = ref(false)
const deleteError = ref('')
const linkCopied = ref(false)
const isSharing = ref(false)
const shareOpen = ref(false)
const shareLink = ref('')
const shareError = ref('')

const inviteesQuery = useGetCollectionInvitees(collectionId, undefined, {
  query: { enabled: computed(() => shareOpen.value && collectionQuery.data.value?.visibility === 'SHARED') },
})

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

// ---------- "Quem pode ver" ----------
const visibilityOpen = ref(false)
const visibilityChoice = ref<'PUBLIC' | 'SHARED' | 'PRIVATE'>('PRIVATE')
const visibilityError = ref('')

const VISIBILITY_OPTIONS = [
  {
    value: 'PRIVATE',
    title: 'Só minha',
    description: 'Ninguém mais vê. É assim que toda coleção começa.',
  },
  {
    value: 'SHARED',
    title: 'Para quem eu escolher',
    description: 'Só as pessoas que você convidar. Não aparece no seu perfil.',
  },
  {
    value: 'PUBLIC',
    title: 'Pública',
    description: 'Aparece no seu perfil. Qualquer pessoa pode ver e seguir.',
  },
]

function openVisibilityDialog(): void {
  if (!collectionQuery.data.value) return
  visibilityChoice.value = collectionQuery.data.value.visibility
  visibilityError.value = ''
  visibilityOpen.value = true
}

const losesFollowers = computed(() => {
  const collection = collectionQuery.data.value
  if (!collection) return false
  const hadFollowers = (collection.followersCount ?? 0) > 0
  return collection.visibility === 'PUBLIC' && visibilityChoice.value !== 'PUBLIC' && hadFollowers
})

const updateVisibilityMutation = useUpdateCollection({
  mutation: {
    onSuccess: (collection) => {
      handleSaved(collection)
      visibilityOpen.value = false
    },
    onError: (error) => {
      visibilityError.value = normalizeHttpError(error).message
    },
  },
})

function saveVisibility(): void {
  const collection = collectionQuery.data.value
  if (!collection || updateVisibilityMutation.isPending.value) return
  visibilityError.value = ''
  updateVisibilityMutation.mutate({
    id: collection.id,
    data: {
      name: collection.name,
      description: collection.description ?? undefined,
      visibility: visibilityChoice.value,
    },
  })
}

// ---------- "Compartilhar" ----------
function inviteUrl(token: string): string {
  return `${window.location.origin}/colecoes/convite/${token}`
}

async function copyText(url: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(url)
    linkCopied.value = true
    setTimeout(() => {
      linkCopied.value = false
    }, 2000)
  } catch {
    // Sem clipboard disponível, o link ainda aparece na tela para copiar manualmente.
  }
}

async function openShareDialog(): Promise<void> {
  const collection = collectionQuery.data.value
  if (!collection) return
  shareError.value = ''
  shareLink.value = ''
  shareOpen.value = true
  if (collection.visibility === 'PUBLIC') {
    shareLink.value = window.location.href
    return
  }
  if (collection.visibility === 'PRIVATE') return
  isSharing.value = true
  try {
    const invite = await getCollectionInviteLink(collection.id)
    shareLink.value = inviteUrl(invite.token)
  } catch (error) {
    shareError.value = normalizeHttpError(error).message
  } finally {
    isSharing.value = false
  }
}

async function regenerateLink(): Promise<void> {
  const collection = collectionQuery.data.value
  if (!collection || isSharing.value) return
  if (!window.confirm('Gerar um novo link? Quem tinha o link antigo perde o acesso.')) return
  shareError.value = ''
  isSharing.value = true
  try {
    const invite = await regenerateCollectionInviteLink(collection.id)
    shareLink.value = inviteUrl(invite.token)
  } catch (error) {
    shareError.value = normalizeHttpError(error).message
  } finally {
    isSharing.value = false
  }
}

// PUT/DELETE /collections/{id}/invitees ainda não passaram pelo orval (mesma
// situação do GET /auth/info e do feed) — chamando direto até rodar o
// backend e `npm run api:generate`.
const inviteUsername = ref('')
const inviteError = ref('')
const isInviting = ref(false)
const removingInviteeId = ref<string | null>(null)

function refreshInvitees(): void {
  void queryClient.invalidateQueries({
    queryKey: getGetCollectionInviteesQueryKey(collectionId, undefined),
  })
}

async function inviteMember(): Promise<void> {
  const collection = collectionQuery.data.value
  const username = inviteUsername.value.trim()
  if (!collection || !username || isInviting.value) return
  inviteError.value = ''
  isInviting.value = true
  try {
    await apiRequest<UserResponse>({
      url: `/collections/${collection.id}/invitees`,
      method: 'PUT',
      data: { username },
    })
    inviteUsername.value = ''
    refreshInvitees()
  } catch (error) {
    inviteError.value = normalizeHttpError(error).message
  } finally {
    isInviting.value = false
  }
}

async function removeInvitee(userId: string): Promise<void> {
  const collection = collectionQuery.data.value
  if (!collection || removingInviteeId.value) return
  removingInviteeId.value = userId
  try {
    await apiRequest<void>({
      url: `/collections/${collection.id}/invitees/${userId}`,
      method: 'DELETE',
    })
    refreshInvitees()
  } catch (error) {
    inviteError.value = normalizeHttpError(error).message
  } finally {
    removingInviteeId.value = null
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
      <RouterLink
        class="collection-details-view__back"
        :to="isOwner ? '/salvos' : `/u/${collectionQuery.data.value.authorUsername}`"
      >
        <AppIcon name="back" :size="18" :stroke-width="2" />
        {{ isOwner ? 'Salvos' : `Perfil de ${collectionQuery.data.value.authorDisplayName}` }}
      </RouterLink>

      <span
        class="collection-details-view__cover"
        aria-hidden="true"
        :style="coverImageUrl ? { backgroundImage: `url(${resolveImageUrl(coverImageUrl)})` } : undefined"
      >
        <AppIcon v-if="!coverImageUrl" name="bookmark" :size="40" :stroke-width="1.5" />
      </span>

      <header class="collection-details-view__header">
        <h1>{{ collectionQuery.data.value.name }}</h1>
        <p v-if="collectionQuery.data.value.description">
          {{ collectionQuery.data.value.description }}
        </p>
        <p class="collection-details-view__meta">
          <template v-if="!isOwner">de @{{ collectionQuery.data.value.authorUsername }} · </template>
          <template v-if="collectionQuery.data.value.publicationsCount">
            {{ collectionQuery.data.value.publicationsCount }}
            {{ collectionQuery.data.value.publicationsCount === 1 ? 'receita' : 'receitas' }}
          </template>
          <template v-else>Nenhuma receita ainda</template>
          <template v-if="isOwner && visibilityMeta">
            ·
            <AppIcon
              :name="visibilityMeta.icon"
              :size="13"
              :stroke-width="2.2"
              class="collection-details-view__meta-icon"
            />
            {{ visibilityMeta.label }}
          </template>
          <template v-if="isOwner && collectionQuery.data.value.followersCount">
            · {{ collectionQuery.data.value.followersCount }}
            {{ collectionQuery.data.value.followersCount === 1 ? 'pessoa segue' : 'pessoas seguem' }}
          </template>
        </p>
        <div class="collection-details-view__actions">
          <CollectionFollowButton
            v-if="!isOwner"
            :collection-id="collectionQuery.data.value.id"
            :following="Boolean(collectionQuery.data.value.followedByCurrentUser)"
            @toggled="refreshCollection"
          />
          <template v-if="isOwner">
            <BaseButton variant="secondary" @click="openShareDialog">Compartilhar</BaseButton>
            <BaseButton variant="ghost" @click="openVisibilityDialog">Quem pode ver</BaseButton>
            <BaseButton variant="ghost" @click="editOpen = true">Editar</BaseButton>
            <BaseButton
              variant="danger"
              :loading="deleteMutation.isPending.value"
              @click="confirmDelete"
            >
              Excluir
            </BaseButton>
          </template>
          <BaseButton v-else variant="secondary" @click="openShareDialog">Compartilhar</BaseButton>
        </div>
        <p v-if="deleteError" class="collection-details-view__error" role="alert">
          {{ deleteError }}
        </p>
      </header>

      <CollectionFormDialog
        v-model:open="editOpen"
        :collection="collectionQuery.data.value"
        hide-visibility
        @saved="handleSaved"
      />

      <BaseDialog
        v-model:open="visibilityOpen"
        title="Quem pode ver"
        description="Você pode mudar isso quando quiser."
      >
        <RadioCardGroup v-model="visibilityChoice" :options="VISIBILITY_OPTIONS" />
        <p v-if="losesFollowers" class="collection-details-view__warning">
          <AppIcon name="alert" :size="18" :stroke-width="1.9" />
          {{ collectionQuery.data.value.followersCount }}
          {{ collectionQuery.data.value.followersCount === 1 ? 'pessoa segue' : 'pessoas seguem' }}
          esta coleção. Se você fechar, {{ collectionQuery.data.value.followersCount === 1 ? 'ela perde' : 'elas perdem' }}
          o acesso e não {{ collectionQuery.data.value.followersCount === 1 ? 'é avisada' : 'são avisadas' }}.
        </p>
        <BaseFieldError v-if="visibilityError" :message="visibilityError" />
        <template #actions>
          <BaseButton variant="ghost" @click="visibilityOpen = false">Cancelar</BaseButton>
          <BaseButton :loading="updateVisibilityMutation.isPending.value" @click="saveVisibility">
            Salvar
          </BaseButton>
        </template>
      </BaseDialog>

      <BaseDialog
        v-model:open="shareOpen"
        title="Compartilhar"
        :description="
          collectionQuery.data.value.visibility === 'PUBLIC'
            ? 'Qualquer pessoa com o link pode ver esta coleção.'
            : collectionQuery.data.value.visibility === 'SHARED'
              ? 'Só quem tem o link consegue ver esta coleção.'
              : 'Convidar alguém torna esta coleção \&quot;Para quem eu escolher\&quot;.'
        "
      >
        <template v-if="collectionQuery.data.value.visibility === 'PRIVATE'">
          <p class="collection-details-view__share-explain">
            Abra "Quem pode ver" e escolha "Para quem eu escolher" para gerar um link de convite.
          </p>
        </template>
        <template v-else>
          <div class="collection-details-view__share-link-row">
            <input
              class="collection-details-view__share-link"
              type="text"
              readonly
              :value="isSharing ? 'Gerando link...' : shareLink"
              @focus="($event.target as HTMLInputElement).select()"
            />
            <BaseButton
              variant="secondary"
              :loading="isSharing"
              @click="copyText(shareLink)"
            >
              {{ linkCopied ? 'Copiado!' : 'Copiar' }}
            </BaseButton>
          </div>
          <BaseFieldError v-if="shareError" :message="shareError" />
          <p
            v-if="collectionQuery.data.value.visibility === 'PUBLIC' && collectionQuery.data.value.followersCount"
            class="collection-details-view__share-followers"
          >
            {{ collectionQuery.data.value.followersCount }}
            {{ collectionQuery.data.value.followersCount === 1 ? 'pessoa segue' : 'pessoas seguem' }}
            esta coleção.
          </p>
          <template v-if="collectionQuery.data.value.visibility === 'SHARED'">
            <BaseButton
              variant="ghost"
              class="collection-details-view__regenerate"
              :loading="isSharing"
              @click="regenerateLink"
            >
              Gerar novo link
            </BaseButton>

            <form class="collection-details-view__invite-row" @submit.prevent="inviteMember">
              <input
                v-model="inviteUsername"
                class="collection-details-view__invite-input"
                type="text"
                placeholder="Nome de usuário"
                aria-label="Convidar pessoa"
              />
              <BaseButton type="submit" variant="secondary" :loading="isInviting">
                Convidar
              </BaseButton>
            </form>
            <BaseFieldError v-if="inviteError" :message="inviteError" />

            <div v-if="inviteesQuery.data.value?.content.length" class="collection-details-view__invitees">
              <p class="collection-details-view__invitees-label">Quem já tem acesso</p>
              <div
                v-for="invitee in inviteesQuery.data.value.content"
                :key="invitee.id"
                class="collection-details-view__invitee"
              >
                <BaseAvatar :name="invitee.displayName" size="small" />
                <span>{{ invitee.displayName }}</span>
                <button
                  type="button"
                  class="collection-details-view__invitee-remove"
                  :disabled="removingInviteeId === invitee.id"
                  @click="removeInvitee(invitee.id)"
                >
                  Remover
                </button>
              </div>
            </div>
          </template>
        </template>
        <template #actions>
          <BaseButton variant="ghost" @click="shareOpen = false">Fechar</BaseButton>
        </template>
      </BaseDialog>

      <div v-if="publicationsQuery.isPending.value" class="collection-details-view__state">
        Carregando publicações...
      </div>
      <div
        v-else-if="!publicationsQuery.data.value?.content.length"
        class="collection-details-view__empty"
      >
        <p v-if="isOwner"><strong>Coleção ainda vazia</strong></p>
        <p v-else>
          <strong>Nada por aqui ainda</strong>
        </p>
        <p v-if="isOwner">Guarde publicações aqui para juntar o que combina.</p>
        <p v-else>
          {{ collectionQuery.data.value.authorDisplayName }} ainda não guardou nada nesta coleção.
          Se você seguir, avisamos quando entrar coisa nova.
        </p>
        <BaseButton v-if="isOwner" @click="router.push('/')">
          Ver o que andaram cozinhando
        </BaseButton>
        <CollectionFollowButton
          v-else-if="!collectionQuery.data.value.followedByCurrentUser"
          :collection-id="collectionQuery.data.value.id"
          :following="false"
          @toggled="refreshCollection"
        />
      </div>
      <div v-else class="collection-details-view__grid">
        <div
          v-for="publication in publicationsQuery.data.value.content"
          :key="publication.id"
          class="collection-details-view__tile"
        >
          <PublicationTile :publication="publication" show-author />
          <button
            v-if="isOwner"
            type="button"
            class="collection-details-view__remove"
            aria-label="Remover da coleção"
            :disabled="removeMutation.isPending.value"
            @click="removePublication(publication.id)"
          >
            <AppIcon name="close" :size="15" :stroke-width="2.4" />
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

.collection-details-view__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-end: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.collection-details-view__back:hover {
  color: var(--color-primary);
}

.collection-details-view__cover {
  display: grid;
  width: 100%;
  height: 9rem;
  margin-block-end: var(--space-6);
  overflow: hidden;
  color: var(--color-primary);
  background-color: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
  background-position: center;
  background-size: cover;
  border-radius: var(--radius-lg);
  place-items: center;
}

.collection-details-view__header {
  margin-block-end: var(--space-10);
}

.collection-details-view h1 {
  margin: 0;
}

.collection-details-view__header > p {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}

.collection-details-view__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.1875rem;
}

.collection-details-view__meta-icon {
  flex: none;
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

.collection-details-view__warning {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-block-start: var(--space-4);
  padding: var(--space-3) var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
  background: color-mix(in srgb, var(--color-danger) 12%, var(--color-surface));
  border-radius: var(--radius-md);
}

.collection-details-view__warning svg {
  flex: none;
}

.collection-details-view__share-link-row {
  display: flex;
  gap: var(--space-2);
}

.collection-details-view__share-link {
  width: 100%;
  min-height: var(--control-min-size);
  padding-inline: var(--space-4);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.collection-details-view__share-explain,
.collection-details-view__share-followers {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.collection-details-view__regenerate {
  margin-block-start: var(--space-3);
}

.collection-details-view__invitees {
  margin-block-start: var(--space-5);
}

.collection-details-view__invitees-label {
  margin-block-end: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.collection-details-view__invitee {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-2);
}

.collection-details-view__invitee span {
  flex: 1;
}

.collection-details-view__invitee-remove {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: none;
  border: 0;
}

.collection-details-view__invitee-remove:hover {
  text-decoration: underline;
}

.collection-details-view__invite-row {
  display: flex;
  gap: var(--space-2);
  margin-block-start: var(--space-4);
}

.collection-details-view__invite-input {
  width: 100%;
  min-height: var(--control-min-size);
  padding-inline: var(--space-4);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.collection-details-view__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(9rem, 1fr));
  gap: var(--space-4) var(--space-3);
}

.collection-details-view__tile {
  position: relative;
}

.collection-details-view__remove {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  display: grid;
  width: 1.75rem;
  height: 1.75rem;
  color: var(--color-primary-contrast);
  background: color-mix(in srgb, black 45%, transparent);
  border: 0;
  border-radius: var(--radius-pill);
  place-items: center;
}

.collection-details-view__empty {
  display: grid;
  gap: var(--space-3);
  justify-items: start;
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: left;
  background: var(--color-surface);
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.collection-details-view__empty p {
  margin: 0;
}

.collection-details-view__empty strong {
  color: var(--color-text);
}

.collection-details-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
