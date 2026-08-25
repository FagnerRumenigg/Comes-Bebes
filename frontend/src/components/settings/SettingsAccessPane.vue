<script setup lang="ts">
import { reactive, ref } from 'vue'

import { normalizeHttpError } from '@/api/errors'
import { useChangePassword } from '@/api/generated/users/users'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import DevicesPanel from '@/components/settings/DevicesPanel.vue'
import { useAuthStore } from '@/stores/auth.store'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'

const authStore = useAuthStore()

const changePasswordDialogOpen = ref(false)
const changePasswordSuccess = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmNewPassword: '' })
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)

const changePasswordMutation = useChangePassword({
  mutation: {
    onSuccess: () => {
      changePasswordDialogOpen.value = false
      changePasswordSuccess.value = true
      form.currentPassword = ''
      form.newPassword = ''
      form.confirmNewPassword = ''
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

function openChangePasswordDialog(): void {
  clearErrors()
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmNewPassword = ''
  changePasswordDialogOpen.value = true
}

function submitChangePassword(): void {
  if (!authStore.identity?.userId || changePasswordMutation.isPending.value) return
  clearErrors()
  if (!form.currentPassword) {
    fieldErrors.currentPassword = 'Informe sua senha atual.'
    return
  }
  if (form.newPassword.length < 8) {
    fieldErrors.newPassword = 'A nova senha precisa ter pelo menos 8 caracteres.'
    return
  }
  if (form.newPassword !== form.confirmNewPassword) {
    fieldErrors.confirmNewPassword = 'As senhas precisam ser iguais.'
    return
  }
  changePasswordMutation.mutate({
    id: authStore.identity.userId,
    data: { currentPassword: form.currentPassword, newPassword: form.newPassword },
  })
}
</script>

<template>
  <div class="settings-access-pane">
    <h2>Entrar e aparelhos</h2>
    <p class="settings-access-pane__lead">Como você entra na sua conta e onde ela está aberta agora.</p>

    <BaseToast
      v-if="changePasswordSuccess"
      title="Senha alterada"
      kind="success"
      @dismiss="changePasswordSuccess = false"
    >
      Da próxima vez, use a nova senha para entrar.
    </BaseToast>

    <SettingsSection title="Senha">
      <SettingsRow title="Sua senha" description="Troque sempre que quiser.">
        <BaseButton variant="secondary" @click="openChangePasswordDialog">Trocar senha</BaseButton>
      </SettingsRow>
    </SettingsSection>

    <DevicesPanel />

    <BaseDialog
      v-model:open="changePasswordDialogOpen"
      title="Trocar senha"
      description="Confirme sua senha atual e escolha a nova."
    >
      <form class="settings-access-pane__form" novalidate @submit.prevent="submitChangePassword">
        <BaseInput
          id="settings-current-password"
          v-model="form.currentPassword"
          type="password"
          label="Senha atual"
          autocomplete="current-password"
          :error="fieldErrors.currentPassword"
          :disabled="changePasswordMutation.isPending.value"
          required
        />
        <BaseInput
          id="settings-new-password"
          v-model="form.newPassword"
          type="password"
          label="Nova senha"
          autocomplete="new-password"
          :error="fieldErrors.newPassword"
          :disabled="changePasswordMutation.isPending.value"
          required
        />
        <BaseInput
          id="settings-confirm-new-password"
          v-model="form.confirmNewPassword"
          type="password"
          label="Confirmar nova senha"
          autocomplete="new-password"
          :error="fieldErrors.confirmNewPassword"
          :disabled="changePasswordMutation.isPending.value"
          required
        />
        <p v-if="generalError" class="settings-access-pane__error" role="alert">{{ generalError }}</p>
      </form>

      <template #actions>
        <BaseButton variant="ghost" @click="changePasswordDialogOpen = false">Cancelar</BaseButton>
        <BaseButton :loading="changePasswordMutation.isPending.value" @click="submitChangePassword">
          Salvar nova senha
        </BaseButton>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.settings-access-pane h2 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-access-pane__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.settings-access-pane__form {
  display: grid;
  gap: var(--space-4);
}

.settings-access-pane__error {
  margin: 0;
  color: var(--color-danger);
}
</style>
