<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { getListQueryKey, useList, useRevoke, useUpdate } from '@/api/generated/devices/devices'
import { getList1QueryKey, useList1, useRemove as useRemoveBiometric } from '@/api/generated/biometric/biometric'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import { normalizeHttpError } from '@/api/errors'
import { isPlatformAuthenticatorAvailable, isWebAuthnSupported, useBiometric } from '@/composables/useBiometric'
import { useAuthStore } from '@/stores/auth.store'

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

const trustMutation = useUpdate({
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

function trustDevice(id: string): void {
  actionError.value = ''
  trustMutation.mutate({ id, data: { isTrusted: true } })
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
  <section class="devices-view">
    <header class="devices-view__header">
      <p class="devices-view__eyebrow">Segurança</p>
      <h1>Meus dispositivos</h1>
      <p>Veja onde sua conta está conectada e revogue o acesso de quem você não reconhece.</p>
    </header>

    <div v-if="devicesQuery.isPending.value" class="devices-view__state">
      Carregando dispositivos...
    </div>
    <div v-else-if="devicesQuery.isError.value" class="devices-view__state" role="alert">
      Não foi possível carregar seus dispositivos.
    </div>
    <div v-else-if="!devicesQuery.data.value?.length" class="devices-view__state">
      Nenhum dispositivo encontrado.
    </div>
    <ul v-else class="devices-view__list">
      <li
        v-for="device in devicesQuery.data.value"
        :key="device.id"
        class="devices-view__item"
        :class="{ 'devices-view__item--current': device.isCurrent }"
      >
        <div class="devices-view__info">
          <div class="devices-view__name">
            <strong>{{ device.deviceName }}</strong>
            <span v-if="device.isCurrent" class="devices-view__badge">Este dispositivo</span>
            <span v-if="device.isTrusted" class="devices-view__badge devices-view__badge--trusted"
              >Confiável</span
            >
            <span v-if="!device.isActive" class="devices-view__badge devices-view__badge--revoked"
              >Revogado</span
            >
          </div>
          <p class="devices-view__meta">Último acesso: {{ formatDate(device.lastActivityAt) }}</p>
        </div>
        <div v-if="device.isActive" class="devices-view__actions">
          <BaseButton
            v-if="!device.isTrusted"
            variant="secondary"
            :loading="trustMutation.isPending.value"
            @click="trustDevice(device.id)"
          >
            Confiar
          </BaseButton>
          <BaseButton
            v-if="!device.isCurrent"
            variant="danger"
            :loading="revokeMutation.isPending.value"
            @click="revokeDevice(device.id)"
          >
            Revogar
          </BaseButton>
        </div>
      </li>
    </ul>

    <p v-if="actionError" class="devices-view__error" role="alert">{{ actionError }}</p>

    <div v-if="biometricsEnabled" class="devices-view__biometric">
      <h2>Biometria neste dispositivo</h2>
      <p>Use Face ID, digital ou Windows Hello para entrar sem digitar a senha neste dispositivo.</p>

      <div v-if="biometricsQuery.isPending.value" class="devices-view__state">Carregando...</div>
      <ul v-else-if="biometricsQuery.data.value?.length" class="devices-view__list">
        <li v-for="biometric in biometricsQuery.data.value" :key="biometric.id" class="devices-view__item">
          <div class="devices-view__info">
            <strong>{{ biometric.biometricType }}</strong>
            <p class="devices-view__meta">Registrada em: {{ formatDate(biometric.registeredAt) }}</p>
          </div>
          <div class="devices-view__actions">
            <BaseButton
              variant="danger"
              :loading="removeBiometricMutation.isPending.value"
              @click="removeBiometric(biometric.id)"
            >
              Remover
            </BaseButton>
          </div>
        </li>
      </ul>
      <BaseButton v-else variant="secondary" :loading="registeringBiometric" @click="registerBiometric">
        Ativar biometria
      </BaseButton>

      <p v-if="biometricError" class="devices-view__error" role="alert">{{ biometricError }}</p>
    </div>

    <div class="devices-view__logout-all">
      <h2>Logout global</h2>
      <p>Encerra a sessão em todos os dispositivos, inclusive este.</p>
      <BaseButton variant="danger" @click="logoutAllDialogOpen = true">
        Logout em todos os dispositivos
      </BaseButton>
    </div>

    <BaseDialog
      v-model:open="logoutAllDialogOpen"
      title="Logout em todos os dispositivos"
      description="Você será desconectado agora, inclusive neste dispositivo. Vai precisar entrar novamente em todos eles."
    >
      <template #actions>
        <BaseButton variant="ghost" @click="logoutAllDialogOpen = false">Cancelar</BaseButton>
        <BaseButton variant="danger" :loading="loggingOutAll" @click="confirmLogoutAll">
          Confirmar logout em todos
        </BaseButton>
      </template>
    </BaseDialog>
  </section>
</template>

<style scoped>
.devices-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.devices-view__header {
  margin-block-end: var(--space-8);
}
.devices-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.devices-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.devices-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.devices-view__list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  list-style: none;
}
.devices-view__item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.devices-view__item--current {
  border-color: var(--color-primary);
}
.devices-view__name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}
.devices-view__badge {
  padding: 0.125rem var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}
.devices-view__badge--trusted {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.devices-view__badge--revoked {
  color: var(--color-danger);
  border-color: var(--color-danger);
}
.devices-view__meta {
  margin-block-start: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.devices-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.devices-view__error {
  margin-block-start: var(--space-4);
  color: var(--color-danger);
}
.devices-view__biometric {
  margin-block-start: var(--space-8);
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.devices-view__biometric h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}
.devices-view__biometric > p:nth-child(2) {
  margin-block: var(--space-2) var(--space-4);
  color: var(--color-text-secondary);
}
.devices-view__logout-all {
  margin-block-start: var(--space-10);
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.devices-view__logout-all h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}
.devices-view__logout-all p {
  margin-block: var(--space-2) var(--space-4);
  color: var(--color-text-secondary);
}
.devices-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
