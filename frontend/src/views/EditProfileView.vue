<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { normalizeHttpError } from '@/api/errors'
import { getFindByUsernameQueryKey, useFindByUsername } from '@/api/generated/profiles/profiles'
import { useUpdateCurrentUser } from '@/api/generated/users/users'
import type { UpdateUserRequest, UserResponse } from '@/api/generated/models'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { useAuthStore } from '@/stores/auth.store'

// bio ainda não passou pelo orval (mesma situação de outros campos novos
// nesta sessão) — trocar pelos tipos gerados depois de rodar o backend e
// `npm run api:generate`.
type ProfileWithBio = UserResponse & { bio?: string | null }
type UpdateUserRequestWithBio = UpdateUserRequest & { bio?: string }

const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const ownUsername = computed(() => authStore.identity?.username ?? '')
const profileQuery = useFindByUsername(ownUsername)

const form = reactive({ displayName: '', username: '', bio: '' })
const initialUsername = ref('')
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)

watch(
  profileQuery.data,
  (profile: ProfileWithBio | undefined) => {
    if (!profile) return
    form.displayName = profile.displayName
    form.username = profile.username
    form.bio = profile.bio ?? ''
    initialUsername.value = profile.username
  },
  { immediate: true },
)

const usernameChanged = computed(
  () => initialUsername.value !== '' && form.username.trim() !== initialUsername.value,
)

const updateMutation = useUpdateCurrentUser({
  mutation: {
    onSuccess: (response) => {
      void queryClient.invalidateQueries({ queryKey: getFindByUsernameQueryKey(ownUsername) })
      void router.push(`/u/${response.username}`)
    },
    onError: (error) => {
      const normalized = normalizeHttpError(error)
      Object.assign(fieldErrors, normalized.fieldErrors)
      generalError.value = normalized.message
    },
  },
})

function clearErrors(): void {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  generalError.value = null
}

function submit(): void {
  if (!authStore.identity?.userId || updateMutation.isPending.value) return
  clearErrors()
  if (!form.displayName.trim()) {
    fieldErrors.displayName = 'Informe como você quer ser chamado.'
    return
  }
  if (!form.username.trim()) {
    fieldErrors.username = 'Informe seu @usuário.'
    return
  }
  const data: UpdateUserRequestWithBio = {
    displayName: form.displayName.trim(),
    username: form.username.trim(),
    bio: form.bio.trim(),
  }
  updateMutation.mutate({ id: authStore.identity.userId, data })
}
</script>

<template>
  <section class="edit-profile-view" aria-labelledby="edit-profile-title">
    <RouterLink class="edit-profile-view__back" :to="`/u/${ownUsername}`">
      <AppIcon name="back" :size="18" :stroke-width="2" />
      Voltar para o perfil
    </RouterLink>

    <div class="edit-profile-view__heading">
      <h1 id="edit-profile-title">Editar perfil</h1>
      <p>É assim que as outras pessoas veem você.</p>
    </div>

    <div v-if="profileQuery.isPending.value" class="edit-profile-view__state">
      Carregando seu perfil...
    </div>
    <template v-else>
      <div class="edit-profile-view__avatar-row">
        <BaseAvatar :name="form.displayName || '?'" size="medium" />
        <div>
          <p class="edit-profile-view__avatar-title">Sua imagem</p>
          <p class="edit-profile-view__avatar-hint">
            Por enquanto é a inicial do seu nome. Em breve dá para escolher um desenho.
          </p>
        </div>
      </div>

      <BaseToast
        v-if="generalError"
        title="Não foi possível salvar"
        kind="error"
        @dismiss="generalError = null"
      >
        {{ generalError }}
      </BaseToast>

      <form class="edit-profile-view__form" novalidate @submit.prevent="submit">
        <BaseInput
          id="edit-profile-display-name"
          v-model="form.displayName"
          label="Como você quer ser chamado"
          autocomplete="name"
          hint="É o nome que aparece nas suas publicações."
          maxlength="100"
          :error="fieldErrors.displayName"
          :disabled="updateMutation.isPending.value"
          required
        />
        <BaseInput
          id="edit-profile-username"
          v-model="form.username"
          label="Seu nome no Comes&Bebes"
          autocomplete="username"
          hint="Serve para as pessoas te encontrarem. Letras, números e sublinhado."
          maxlength="30"
          :error="fieldErrors.username"
          :disabled="updateMutation.isPending.value"
          required
        >
          <template #lead>@</template>
        </BaseInput>
        <p v-if="usernameChanged" class="edit-profile-view__username-warning">
          <AppIcon name="alert" :size="18" :stroke-width="1.9" />
          Trocando para <strong>@{{ form.username.trim() }}</strong
          >, os links antigos param de funcionar. Guardamos <strong>@{{ initialUsername }}</strong>
          por 30 dias, para ninguém usar no seu lugar.
        </p>
        <BaseTextarea
          id="edit-profile-bio"
          v-model="form.bio"
          label="Descrição"
          hint="Um pouco sobre você — aparece no seu perfil."
          maxlength="280"
          :rows="3"
          :disabled="updateMutation.isPending.value"
        />

        <div class="edit-profile-view__actions">
          <BaseButton variant="ghost" type="button" @click="router.push(`/u/${ownUsername}`)">
            Cancelar
          </BaseButton>
          <BaseButton type="submit" :loading="updateMutation.isPending.value">Salvar</BaseButton>
        </div>
      </form>
    </template>
  </section>
</template>

<style scoped>
.edit-profile-view {
  display: grid;
  max-width: var(--content-narrow);
  gap: var(--space-6);
  padding-block: var(--space-6);
  margin-inline: auto;
}

.edit-profile-view__back {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.edit-profile-view__back:hover {
  color: var(--color-primary);
}

.edit-profile-view__heading p {
  margin-block-start: var(--space-1);
  color: var(--color-text-secondary);
}

.edit-profile-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.edit-profile-view__avatar-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.edit-profile-view__avatar-title {
  margin: 0;
  font-weight: var(--font-weight-semibold);
}

.edit-profile-view__avatar-hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.edit-profile-view__form {
  display: grid;
  gap: var(--space-5);
}

.edit-profile-view__username-warning {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-block-start: calc(var(--space-3) * -1);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
  background: var(--color-surface);
  border: 1.5px solid var(--color-secondary);
  border-radius: var(--radius-md);
}

.edit-profile-view__username-warning svg {
  flex: none;
  color: var(--color-secondary);
}

.edit-profile-view__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>
