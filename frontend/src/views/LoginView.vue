<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useLogin } from '@/api/generated/authentication/authentication'
import type { LoginRequest, LoginResponse } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCheckbox from '@/components/base/BaseCheckbox.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import { isPlatformAuthenticatorAvailable, isWebAuthnSupported, useBiometric } from '@/composables/useBiometric'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'
import { mockCredentials } from '@/mocks/fixtures/auth'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { checkStatus, authenticate, register } = useBiometric()

const form = reactive<LoginRequest>({ username: '', password: '' })
const remember = ref(false)
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)
const mocksEnabled = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS !== 'false'

const biometricAvailable = ref(false)
const biometricPending = ref(false)
const biometricPromptOpen = ref(false)
const enrollingBiometricAfterLogin = ref(false)
let pendingLoginResponse: LoginResponse | null = null

if (typeof route.query.username === 'string') form.username = route.query.username

function goToDestination(onboardingCompleted: boolean): void {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
  const target = onboardingCompleted ? safeRedirect : '/onboarding'
  void router.replace(target)
}

async function maybeOfferBiometricEnrollment(response: LoginResponse): Promise<void> {
  if (!isWebAuthnSupported()) {
    goToDestination(response.onboardingCompleted)
    return
  }
  const [platformReady, alreadyRegistered] = await Promise.all([
    isPlatformAuthenticatorAvailable(),
    checkStatus(response.deviceId).catch(() => true),
  ])
  if (!platformReady || alreadyRegistered) {
    goToDestination(response.onboardingCompleted)
    return
  }
  pendingLoginResponse = response
  biometricPromptOpen.value = true
}

watch(biometricPromptOpen, (open) => {
  if (open) return
  const pending = pendingLoginResponse
  pendingLoginResponse = null
  if (pending) goToDestination(pending.onboardingCompleted)
})

async function confirmBiometricEnrollment(): Promise<void> {
  if (!pendingLoginResponse) return
  enrollingBiometricAfterLogin.value = true
  try {
    await register(pendingLoginResponse.deviceId)
  } catch {
    // O login já foi concluído; falha ao ativar biometria não deve travar o usuário.
  } finally {
    enrollingBiometricAfterLogin.value = false
    biometricPromptOpen.value = false
  }
}

const loginMutation = useLogin({
  mutation: {
    onSuccess(response) {
      authStore.acceptSession(response, remember.value)
      void maybeOfferBiometricEnrollment(response)
    },
    onError(error) {
      const normalizedError = normalizeHttpError(error)
      Object.assign(fieldErrors, normalizedError.fieldErrors)
      generalError.value = normalizedError.message
    },
  },
})

onMounted(async () => {
  const deviceId = authStore.deviceId
  if (!deviceId || !isWebAuthnSupported()) return
  const [platformReady, hasBiometric] = await Promise.all([
    isPlatformAuthenticatorAvailable(),
    checkStatus(deviceId).catch(() => false),
  ])
  biometricAvailable.value = platformReady && hasBiometric
})

async function submitBiometric(): Promise<void> {
  const deviceId = authStore.deviceId
  if (!deviceId || biometricPending.value) return
  biometricPending.value = true
  generalError.value = null
  try {
    const response = await authenticate(deviceId)
    authStore.acceptSession(response, remember.value)
    goToDestination(response.onboardingCompleted)
  } catch (error) {
    generalError.value = normalizeHttpError(error).message
  } finally {
    biometricPending.value = false
  }
}

function clearErrors(): void {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  generalError.value = null
}

function submit(): void {
  if (loginMutation.isPending.value) return
  clearErrors()

  if (!form.username.trim()) fieldErrors.username = 'Informe seu nome de usuário.'
  if (!form.password) fieldErrors.password = 'Informe sua senha.'
  if (Object.keys(fieldErrors).length > 0) return

  loginMutation.mutate({
    data: {
      username: form.username.trim(),
      password: form.password,
    },
  })
}
</script>

<template>
  <section class="auth-view" aria-labelledby="login-title">
    <div class="auth-view__heading">
      <p class="auth-view__eyebrow">Que bom ter você de volta</p>
      <h1 id="login-title">Bem-vindo de volta</h1>
      <p>Entre para continuar compartilhando suas descobertas culinárias.</p>
    </div>

    <BaseToast
      v-if="route.query.registered"
      title="Cadastro concluído"
      kind="success"
      :dismissible="false"
    >
      Sua conta foi criada. Agora você já pode entrar.
    </BaseToast>

    <BaseToast
      v-if="generalError"
      title="Não foi possível entrar"
      kind="error"
      @dismiss="generalError = null"
    >
      {{ generalError }}
    </BaseToast>

    <BaseButton
      v-if="biometricAvailable"
      class="auth-form__biometric"
      variant="secondary"
      :loading="biometricPending"
      @click="submitBiometric"
    >
      Entrar com biometria
    </BaseButton>

    <p v-if="biometricAvailable" class="auth-view__divider" role="separator">ou entre com sua senha</p>

    <form class="auth-form" novalidate @submit.prevent="submit">
      <BaseInput
        id="login-username"
        v-model="form.username"
        label="Nome de usuário"
        autocomplete="username"
        placeholder="Ex.: maria_cozinha"
        :error="fieldErrors.username"
        :disabled="loginMutation.isPending.value"
        required
      />
      <PasswordInput
        id="login-password"
        v-model="form.password"
        label="Senha"
        autocomplete="current-password"
        placeholder="Digite sua senha"
        :error="fieldErrors.password"
        :disabled="loginMutation.isPending.value"
        required
      />
      <BaseCheckbox
        id="remember-session"
        v-model="remember"
        label="Lembrar-me neste dispositivo"
        :disabled="loginMutation.isPending.value"
      />
      <BaseButton class="auth-form__submit" type="submit" :loading="loginMutation.isPending.value">
        Entrar na conta
      </BaseButton>
    </form>

    <details v-if="mocksEnabled" class="mock-credentials">
      <summary>Credenciais para testar sem backend</summary>
      <p>
        Usuário: <code>{{ mockCredentials.user.username }}</code
        ><br />
        Senha: <code>{{ mockCredentials.user.password }}</code>
      </p>
    </details>

    <p class="auth-view__alternative">
      Ainda não tem uma conta?
      <RouterLink to="/cadastro">Criar cadastro</RouterLink>
    </p>

    <BaseDialog
      v-model:open="biometricPromptOpen"
      title="Ativar biometria?"
      description="Use Face ID, digital ou Windows Hello para entrar mais rápido neste dispositivo da próxima vez."
    >
      <template #actions>
        <BaseButton variant="ghost" @click="biometricPromptOpen = false">Agora não</BaseButton>
        <BaseButton
          variant="primary"
          :loading="enrollingBiometricAfterLogin"
          @click="confirmBiometricEnrollment"
        >
          Ativar
        </BaseButton>
      </template>
    </BaseDialog>
  </section>
</template>

<style scoped>
.auth-view {
  display: grid;
  gap: var(--space-6);
}

.auth-view__heading {
  display: grid;
  gap: var(--space-2);
}

.auth-view__heading > p:last-child {
  color: var(--color-text-secondary);
}

.auth-view__eyebrow {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.auth-form__biometric {
  width: 100%;
}

.auth-view__divider {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: center;
}

.auth-form {
  display: grid;
  gap: var(--space-5);
}

.auth-form__submit {
  width: 100%;
  margin-block-start: var(--space-2);
}

.mock-credentials {
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.mock-credentials summary {
  color: var(--color-text);
  cursor: pointer;
  font-weight: var(--font-weight-medium);
}

.mock-credentials p {
  margin-block-start: var(--space-2);
}

.auth-view__alternative {
  color: var(--color-text-secondary);
  text-align: center;
}

.auth-view__alternative a {
  font-weight: var(--font-weight-semibold);
}
</style>
