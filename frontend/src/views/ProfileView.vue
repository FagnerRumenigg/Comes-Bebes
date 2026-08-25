<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { getFindByUsernameQueryKey, useFindByUsername } from '@/api/generated/profiles/profiles'
import type { UserResponse } from '@/api/generated/models'
import {
  getGetUserCollectionsQueryKey,
  useBlock,
  useGetUserCollections,
  useGetUserPublications,
} from '@/api/generated/users/users'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import CollectionCard from '@/components/collection/CollectionCard.vue'
import CollectionFormDialog from '@/components/collection/CollectionFormDialog.vue'
import FollowButton from '@/components/profile/FollowButton.vue'
import PublicationTile from '@/components/publication/PublicationTile.vue'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

// bio ainda não passou pelo orval (mesma situação de coverImageUrls) —
// trocar por UserResponse puro depois de rodar o backend e `npm run
// api:generate`.
type ProfileWithBio = UserResponse & { bio?: string | null }

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const username = computed(() => String(route.params.username ?? ''))
const profileQuery = useFindByUsername(username)
const profile = computed(() => profileQuery.data.value as ProfileWithBio | undefined)
const userId = computed(() => profileQuery.data.value?.id ?? '')
const publicationsQuery = useGetUserPublications(userId, { page: 1, size: 20 })
const collectionsQuery = useGetUserCollections(userId, { page: 1, size: 20 })
const newCollectionOpen = ref(false)

function handleCollectionCreated(): void {
  void queryClient.invalidateQueries({ queryKey: getGetUserCollectionsQueryKey(userId) })
}

const blockDialogOpen = ref(false)
const blockReason = ref('')
const blockError = ref('')

// "Coleções públicas" — mostra só as abertas, mesmo no próprio perfil
// (produto5.md: coleções privadas/compartilhadas ficam em Salvos, nunca
// aparecem na vitrine pública, nem para quem é dono).
const publicCollections = computed(
  () =>
    collectionsQuery.data.value?.content.filter(
      (collection) => collection.visibility === 'PUBLIC',
    ) ?? [],
)

const canBlock = computed(
  () =>
    authStore.isAdmin &&
    Boolean(profileQuery.data.value) &&
    profileQuery.data.value?.id !== authStore.identity?.userId &&
    profileQuery.data.value?.status !== 'BLOCKED',
)

const isOwnProfile = computed(
  () =>
    Boolean(profileQuery.data.value) &&
    profileQuery.data.value?.id === authStore.identity?.userId,
)

const showFollowButton = computed(() => Boolean(profileQuery.data.value) && !isOwnProfile.value)

function refreshProfile(): void {
  void queryClient.invalidateQueries({ queryKey: getFindByUsernameQueryKey(username) })
}

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
        <BaseAvatar :name="profileQuery.data.value.displayName" size="large" />
        <h1>{{ profileQuery.data.value.displayName }}</h1>
        <p class="profile-view__username">@{{ profileQuery.data.value.username }}</p>
        <p v-if="profile?.bio" class="profile-view__bio">{{ profile.bio }}</p>
        <p v-else-if="isOwnProfile" class="profile-view__bio profile-view__bio--empty">
          Conte um pouco sobre você.
          <RouterLink to="/perfil/editar">Adicionar descrição</RouterLink>
        </p>
        <div class="profile-view__actions">
          <BaseButton v-if="isOwnProfile" variant="secondary" @click="router.push('/perfil/editar')">
            Editar perfil
          </BaseButton>
          <FollowButton
            v-if="showFollowButton"
            class="profile-view__follow"
            :user-id="profileQuery.data.value.id"
            :following="Boolean(profileQuery.data.value.followedByCurrentUser)"
            @toggled="refreshProfile"
          />
          <RouterLink class="profile-view__following-link" :to="`/u/${username}/seguindo`">
            {{ isOwnProfile ? 'Quem eu sigo' : `Quem ${profileQuery.data.value.displayName} segue` }}
          </RouterLink>
        </div>
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

      <section
        v-if="!collectionsQuery.isPending.value && (publicCollections.length || isOwnProfile)"
        class="profile-view__section"
      >
        <div class="profile-view__section-header">
          <div>
            <h2>Coleções públicas</h2>
            <p v-if="isOwnProfile" class="profile-view__section-hint">
              Só as abertas aparecem aqui.
            </p>
          </div>
          <BaseButton v-if="isOwnProfile" variant="secondary" @click="newCollectionOpen = true">
            Nova coleção
          </BaseButton>
        </div>
        <div v-if="publicCollections.length" class="profile-view__grid">
          <CollectionCard
            v-for="collection in publicCollections"
            :key="collection.id"
            :collection="collection"
          />
        </div>
        <div v-else-if="isOwnProfile" class="profile-view__empty">
          <p><strong>Nenhuma coleção aberta</strong></p>
          <p>
            Suas coleções ficam em Salvos. Se abrir alguma, ela aparece aqui para quem visitar
            seu perfil.
          </p>
          <BaseButton variant="ghost" @click="router.push('/salvos')">Ir para Salvos</BaseButton>
        </div>
      </section>

      <CollectionFormDialog v-model:open="newCollectionOpen" @saved="handleCollectionCreated" />

      <section class="profile-view__section">
        <h2>Publicações</h2>
        <div v-if="publicationsQuery.isPending.value" class="profile-view__state">
          Carregando publicações...
        </div>
        <div
          v-else-if="!publicationsQuery.data.value?.content.length"
          class="profile-view__empty"
        >
          <p v-if="isOwnProfile"><strong>Nada publicado ainda</strong></p>
          <p v-else>
            <strong>{{ profileQuery.data.value.displayName }} ainda não publicou nada.</strong>
          </p>
          <BaseButton v-if="isOwnProfile" @click="router.push('/publicar')">
            Publicar algo que você fez
          </BaseButton>
        </div>
        <div v-else class="profile-view__grid">
          <PublicationTile
            v-for="publication in publicationsQuery.data.value.content"
            :key="publication.id"
            :publication="publication"
          />
        </div>
      </section>
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

.profile-view h1 {
  margin: var(--space-4) 0 0;
  font-size: clamp(2rem, 5vw, 3.5rem);
}

.profile-view__username {
  margin-block-start: var(--space-2);
  color: var(--color-text-secondary);
}

.profile-view__bio {
  max-width: 44em;
  margin-block-start: var(--space-3);
  line-height: var(--line-height-body);
}

.profile-view__bio--empty {
  color: var(--color-text-secondary);
  font-style: italic;
}

.profile-view__bio--empty a {
  margin-inline-start: var(--space-1);
  font-style: normal;
}

.profile-view__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-block-start: var(--space-4);
}

.profile-view__following-link {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
}

.profile-view__following-link:hover {
  text-decoration: underline;
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

.profile-view__section {
  margin-block-end: var(--space-10);
}

.profile-view__section-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  margin-block-end: var(--space-4);
}

.profile-view__section h2 {
  margin: 0 0 var(--space-4);
  font-size: var(--font-size-xl);
}

.profile-view__section-header h2 {
  margin-block-end: 0;
}

.profile-view__section-hint {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.profile-view__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(9rem, 1fr));
  gap: var(--space-4) var(--space-3);
}

.profile-view__empty {
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

.profile-view__empty p {
  margin: 0;
}

.profile-view__empty strong {
  color: var(--color-text);
}

.profile-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
