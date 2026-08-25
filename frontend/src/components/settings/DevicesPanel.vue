<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { getListQueryKey, useList, useRevoke } from '@/api/generated/devices/devices'
import { getList1QueryKey, useList1, useRemove as useRemoveBiometric } from '@/api/generated/biometric/biometric'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import { normalizeHttpError } from '@/api/errors'
import { isPlatformAuthenticatorAvailable, isWebAuthnSupported, useBiometric } from '@/composables/useBiometric'
import { useAuthStore } from '@/stores/auth.store'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'
import SettingsZone from './SettingsZone.vue'

const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const { register: registerBiometricCredential } = useBiometric()

const devicesQuery = useList()
const actionError = ref('')
const logoutAllDialogOpen = ref(false)
const loggingOutAll = ref(false)

const biometricSupported = ref(false)
const registeringBiometric = ref(false)
const biometricError = ref('')

onMounted(async () => {
  if (!isWebAuthnSupported() || !authStore.deviceId) return
  biometricSupported.value = await isPlatformAuthenticatorAvailable()
})

const biometricsEnabled = computed(() => biometricSupported.value && !!authStore.deviceId)
const biometricsQuery = useList1(
  computed(() => ({ deviceId: authStore.deviceId ?? '' })),
  { query: { enabled: biometricsEnabled } },
)

function invalidateBiometrics(): void {
  if (!authStore.deviceId) return
  void queryClient.invalidateQueries({ queryKey: getList1QueryKey({ deviceId: authStore.deviceId }) })
}

const removeBiometricMutation = useRemoveBiometric({
  mutation: {
    onSuccess: invalidateBiometrics,
    onError: (error) => {
      biometricError.value = normalizeHttpError(error).message
    },
  },
})

async function registerBiometric(): Promise<void> {
  if (!authStore.deviceId || registeringBiometric.value) return
  registeringBiometric.value = true
  biometricError.value = ''
  try {
    await registerBiometricCredential(authStore.deviceId)
    invalidateBiometrics()
  } catch (error) {
    biometricError.value = normalizeHttpError(error).message
  } finally {
    registeringBiometric.value = false
  }
}

function removeBiometric(id: string): void {
  biometricError.value = ''
  removeBiometricMutation.mutate({ id })
}

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

function invalidateDevices(): void {
  void queryClient.invalidateQueries({ queryKey: getListQueryKey() })
}

const revokeMutation = useRevoke({
  mutation: {
    onSuccess: invalidateDevices,
    onError: (error) => {
      actionError.value = normalizeHttpError(error).message
    },
  },
})

function revokeDevice(id: string): void {
  actionError.value = ''
  revokeMutation.mutate({ id })
}

async function confirmLogoutAll(): Promise<void> {
  loggingOutAll.value = true
  try {
    await authStore.logoutAll()
    logoutAllDialogOpen.value = false
    await router.push('/login')
  } finally {
    loggingOutAll.value = false
  }
}
</script>

<template>
  <div class="devices-panel">
    <SettingsSection
      title="Entrar com a digital"
      note="Use a digital ou o reconhecimento facial do seu aparelho para entrar sem digitar a senha. Funciona só no celular."
    >
      <SettingsRow
        title="Neste aparelho"
        :description="
          !biometricsEnabled
            ? 'Não disponível neste aparelho'
            : biometricsQuery.data.value?.length
              ? undefined
              : 'Ainda não está ativado'
        "
      >
        <BaseButton
          v-if="biometricsEnabled && !biometricsQuery.data.value?.length"
          variant="secondary"
          :loading="registeringBiometric"
          @click="registerBiometric"
        >
          Ativar
        </BaseButton>
      </SettingsRow>

      <template v-if="biometricsEnabled && biometricsQuery.data.value?.length">
        <SettingsRow
          v-for="biometric in biometricsQuery.data.value"
          :key="biometric.id"
          :title="biometric.biometricType"
          :description="`Registrada em ${formatDate(biometric.registeredAt)}`"
        >
          <BaseButton variant="danger" :loading="removeBiometricMutation.isPending.value" @click="removeBiometric(biometric.id)">
            Remover
          </BaseButton>
        </SettingsRow>
      </template>
      <p v-if="biometricError" class="devices-panel__error" role="alert">{{ biometricError }}</p>
    </SettingsSection>

    <SettingsSection title="Onde sua conta está aberta" note="Se você não reconhecer algum, desconecte.">
      <div v-if="devicesQuery.isPending.value" class="devices-panel__state">Carregando dispositivos...</div>
      <div v-else-if="devicesQuery.isError.value" class="devices-panel__state" role="alert">
        Não foi possível carregar seus dispositivos.
      </div>
      <template v-else>
        <SettingsRow
          v-for="device in devicesQuery.data.value"
          :key="device.id"
          :title="device.isCurrent ? `${device.deviceName} · Este aparelho` : device.deviceName"
          :description="device.isActive ? `Último acesso: ${formatDate(device.lastActivityAt)}` : 'Revogado'"
        >
          <BaseButton
            v-if="device.isActive && !device.isCurrent"
            variant="secondary"
            :loading="revokeMutation.isPending.value"
            @click="revokeDevice(device.id)"
          >
            Desconectar
          </BaseButton>
        </SettingsRow>
      </template>
      <p v-if="actionError" class="devices-panel__error" role="alert">{{ actionError }}</p>
    </SettingsSection>

    <SettingsZone title="Se achar que alguém entrou na sua conta">
      <SettingsRow
        title="Sair de todos os aparelhos"
        description="Você vai precisar entrar de novo em todos, inclusive neste. Troque a senha depois."
      >
        <BaseButton variant="danger" @click="logoutAllDialogOpen = true">Sair de todos</BaseButton>
      </SettingsRow>
    </SettingsZone>

    <BaseDialog
      v-model:open="logoutAllDialogOpen"
      title="Sair de todos os aparelhos"
      description="Você será desconectado agora, inclusive neste aparelho. Vai precisar entrar novamente em todos eles."
    >
      <template #actions>
        <BaseButton variant="ghost" @click="logoutAllDialogOpen = false">Cancelar</BaseButton>
        <BaseButton variant="danger" :loading="loggingOutAll" @click="confirmLogoutAll">
          Confirmar saída de todos
        </BaseButton>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.devices-panel__state {
  padding: var(--space-6);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.devices-panel__error {
  margin-block-start: var(--space-3);
  color: var(--color-danger);
}
</style>
