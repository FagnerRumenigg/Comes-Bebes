<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { normalizeHttpError } from '@/api/errors'
import { useFindByUsername } from '@/api/generated/profiles/profiles'
import { useAnonymize, useUpdateCurrentUser } from '@/api/generated/users/users'
import type { UpdateUserRequest, UserResponse } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import RadioCardGroup from '@/components/base/RadioCardGroup.vue'
import { getAccountInfoQueryKey, useAccountInfo } from '@/composables/useAccountInfo'
import { PUBLICATION_VISIBILITY_OPTIONS } from '@/features/publications/visibilityOptions'
import { useAuthStore } from '@/stores/auth.store'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'
import SettingsZone from './SettingsZone.vue'

// bio ainda não passou pelo orval (mesma situação de outros campos novos
// nesta sessão) — trocar por UserResponse puro depois de rodar o backend e
// `npm run api:generate`.
type ProfileWithBio = UserResponse & { bio?: string | null }
type UpdateUserRequestWithVisibility = UpdateUserRequest & { defaultPublicationVisibility?: string }

const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const ownUsername = computed(() => authStore.identity?.username ?? '')
const profileQuery = useFindByUsername(ownUsername)
const profile = computed(() => profileQuery.data.value as ProfileWithBio | undefined)

function goToEditProfile(): void {
  void router.push('/perfil/editar')
}

// "Quem pode ver o que você publica" (docs/telas/09-configuracoes.html) —
// vale só pras próximas publicações; CreatePublicationView.vue lê essa
// mesma preferência pra pré-selecionar o padrão.
const { defaultPublicationVisibility } = useAccountInfo()
const visibilityChoice = ref<'PUBLIC' | 'INTERNAL' | 'PRIVATE'>('PUBLIC')
const visibilityError = ref('')

watch(
  defaultPublicationVisibility,
  (value) => {
    if (value) visibilityChoice.value = value
  },
  { immediate: true },
)

const updateVisibilityMutation = useUpdateCurrentUser({
  mutation: {
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: getAccountInfoQueryKey(authStore.identity?.userId ?? null),
      })
    },
    onError: (error) => {
      visibilityError.value = normalizeHttpError(error).message
    },
  },
})

function updateVisibility(value: string): void {
  if (!authStore.identity?.userId || updateVisibilityMutation.isPending.value) return
  visibilityError.value = ''
  visibilityChoice.value = value as 'PUBLIC' | 'INTERNAL' | 'PRIVATE'
  const data: UpdateUserRequestWithVisibility = { defaultPublicationVisibility: value }
  updateVisibilityMutation.mutate({ id: authStore.identity.userId, data })
}

const emailDialogOpen = ref(false)
const emailForm = reactive({ email: '' })
const emailFieldErrors = reactive<Record<string, string>>({})
const emailGeneralError = ref<string | null>(null)

const updateEmailMutation = useUpdateCurrentUser({
  mutation: {
    onSuccess: () => {
      emailDialogOpen.value = false
      emailForm.email = ''
    },
    onError: (error) => {
      const normalized = normalizeHttpError(error)
      Object.assign(emailFieldErrors, normalized.fieldErrors)
      emailGeneralError.value = normalized.message
    },
  },
})

function openEmailDialog(): void {
  for (const key of Object.keys(emailFieldErrors)) delete emailFieldErrors[key]
  emailGeneralError.value = null
  emailForm.email = ''
  emailDialogOpen.value = true
}

function submitEmail(): void {
  if (!authStore.identity?.userId || updateEmailMutation.isPending.value) return
  for (const key of Object.keys(emailFieldErrors)) delete emailFieldErrors[key]
  emailGeneralError.value = null
  if (!emailForm.email.trim()) {
    emailFieldErrors.email = 'Informe o novo e-mail.'
    return
  }
  updateEmailMutation.mutate({ id: authStore.identity.userId, data: { email: emailForm.email.trim() } })
}

async function logout(): Promise<void> {
  await authStore.logout()
  await router.push('/')
}

const deleteAccountDialogOpen = ref(false)
const deletingAccount = ref(false)
const deleteAccountError = ref<string | null>(null)

const anonymizeMutation = useAnonymize({
  mutation: {
    onError: (error) => {
      deleteAccountError.value = normalizeHttpError(error).message
    },
  },
})

async function confirmDeleteAccount(): Promise<void> {
  if (!authStore.identity?.userId) return
  deletingAccount.value = true
  deleteAccountError.value = null
  try {
    await anonymizeMutation.mutateAsync({ id: authStore.identity.userId })
    deleteAccountDialogOpen.value = false
    await authStore.logout()
    await router.push('/')
  } catch {
    // erro já exposto via deleteAccountError
  } finally {
    deletingAccount.value = false
  }
}
</script>

<template>
  <div class="settings-account-pane">
    <h2>Minha conta</h2>
    <p class="settings-account-pane__lead">Seus dados e como você aparece para as outras pessoas.</p>

    <SettingsSection>
      <SettingsRow title="Como você quer ser chamado" :description="profileQuery.data.value?.displayName">
        <BaseButton variant="secondary" @click="goToEditProfile">Mudar</BaseButton>
      </SettingsRow>
      <SettingsRow
        title="Seu nome no Comes&Bebes"
        :description="profileQuery.data.value ? `@${profileQuery.data.value.username}` : undefined"
      >
        <BaseButton variant="secondary" @click="goToEditProfile">Mudar</BaseButton>
      </SettingsRow>
      <SettingsRow title="E-mail" description="Por enquanto não conseguimos mostrar seu e-mail atual aqui.">
        <BaseButton variant="secondary" @click="openEmailDialog">Mudar</BaseButton>
      </SettingsRow>
      <SettingsRow
        title="Descrição"
        :description="profile?.bio || 'Ainda não preenchida.'"
      >
        <BaseButton variant="secondary" @click="goToEditProfile">Mudar</BaseButton>
      </SettingsRow>
    </SettingsSection>

    <SettingsSection
      title="Quem pode ver o que você publica"
      note="Vale para as próximas publicações. As antigas continuam como estão, e dá para mudar uma por uma."
    >
      <RadioCardGroup
        :model-value="visibilityChoice"
        :options="PUBLICATION_VISIBILITY_OPTIONS"
        @update:model-value="updateVisibility"
      />
      <BaseFieldError v-if="visibilityError" :message="visibilityError" />
    </SettingsSection>

    <SettingsZone title="Encerrar">
      <SettingsRow title="Sair da conta" description="Só neste aparelho">
        <BaseButton variant="secondary" @click="logout">Sair</BaseButton>
      </SettingsRow>
      <SettingsRow
        title="Apagar minha conta"
        description="Sua conta deixa de existir. Suas publicações continuam no site, mas sem seu nome. Não dá para desfazer."
      >
        <BaseButton variant="danger" @click="deleteAccountDialogOpen = true">Apagar</BaseButton>
      </SettingsRow>
    </SettingsZone>

    <BaseDialog
      v-model:open="emailDialogOpen"
      title="Mudar e-mail"
      description="É por ele que você entra na sua conta a partir de agora."
    >
      <form class="settings-account-pane__form" novalidate @submit.prevent="submitEmail">
        <BaseInput
          id="settings-account-email"
          v-model="emailForm.email"
          type="email"
          label="Novo e-mail"
          autocomplete="email"
          :error="emailFieldErrors.email"
          :disabled="updateEmailMutation.isPending.value"
          required
        />
        <p v-if="emailGeneralError" class="settings-account-pane__error" role="alert">{{ emailGeneralError }}</p>
      </form>
      <template #actions>
        <BaseButton variant="ghost" @click="emailDialogOpen = false">Cancelar</BaseButton>
        <BaseButton :loading="updateEmailMutation.isPending.value" @click="submitEmail">Salvar</BaseButton>
      </template>
    </BaseDialog>

    <BaseDialog
      v-model:open="deleteAccountDialogOpen"
      title="Apagar minha conta"
      description="Sua conta deixa de existir e você é desconectado. Suas publicações continuam no site, mas sem seu nome. Não dá para desfazer."
    >
      <p v-if="deleteAccountError" class="settings-account-pane__error" role="alert">{{ deleteAccountError }}</p>
      <template #actions>
        <BaseButton variant="ghost" @click="deleteAccountDialogOpen = false">Cancelar</BaseButton>
        <BaseButton variant="danger" :loading="deletingAccount" @click="confirmDeleteAccount">
          Apagar minha conta
        </BaseButton>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.settings-account-pane h2 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-account-pane__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.settings-account-pane__form {
  display: grid;
  gap: var(--space-4);
}

.settings-account-pane__error {
  margin: 0;
  color: var(--color-danger);
}
</style>
