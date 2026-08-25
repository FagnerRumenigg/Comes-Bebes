<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useLogin } from '@/api/generated/authentication/authentication'
import type { LoginRequest, LoginResponse } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCheckbox from '@/components/base/BaseCheckbox.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { isPlatformAuthenticatorAvailable, isWebAuthnSupported, useBiometric } from '@/composables/useBiometric'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'
import { mockCredentials } from '@/mocks/fixtures/auth'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { checkStatus, authenticate, register } = useBiometric()

const form = reactive<LoginRequest>({ identifier: '', password: '' })
const remember = ref(false)
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)
const credentialsInvalid = ref(false)
const mocksEnabled = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS !== 'false'

// Entrada por digital só faz sentido no celular (docs/telas/02) — no
// desktop a senha do SO não protege o navegador do jeito que protege o app.
const MOBILE_BREAKPOINT_QUERY = '(max-width: 48rem)'
const isMobile = ref(false)
const showPasswordForm = ref(false)
let mobileMediaQuery: MediaQueryList | undefined

function handleMobileMediaChange(event: MediaQueryListEvent): void {
  isMobile.value = event.matches
}

onMounted(() => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
  mobileMediaQuery = window.matchMedia(MOBILE_BREAKPOINT_QUERY)
  isMobile.value = mobileMediaQuery.matches
  mobileMediaQuery.addEventListener('change', handleMobileMediaChange)
})

onBeforeUnmount(() => {
  mobileMediaQuery?.removeEventListener('change', handleMobileMediaChange)
})

const biometricAvailable = ref(false)
const biometricPending = ref(false)
const biometricPromptOpen = ref(false)
const enrollingBiometricAfterLogin = ref(false)
let pendingLoginResponse: LoginResponse | null = null

const BIOMETRIC_PROMPT_DISMISSED_KEY = 'comes-e-bebes:hide-biometric-enrollment-prompt'

function isBiometricPromptDismissedForever(): boolean {
  try {
    return window.localStorage.getItem(BIOMETRIC_PROMPT_DISMISSED_KEY) === 'true'
  } catch {
    return false
  }
}

function dismissBiometricPromptForever(): void {
  try {
    window.localStorage.setItem(BIOMETRIC_PROMPT_DISMISSED_KEY, 'true')
  } catch {
    // Sem storage disponível, só fecha o modal desta vez.
  }
  biometricPromptOpen.value = false
}

if (typeof route.query.email === 'string') form.identifier = route.query.email

function goToDestination(onboardingCompleted: boolean): void {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
  const target = onboardingCompleted ? safeRedirect : '/bem-vindo'
  void router.replace(target)
}

/**
 * isUserVerifyingPlatformAuthenticatorAvailable() pode nunca resolver em alguns
 * navegadores/ambientes sem autenticador (ex.: Chromium headless sem virtual
 * authenticator) — sem um limite, isso travaria o redirecionamento pós-login
 * indefinidamente. checkStatus() já tem seu próprio .catch(), mas não protege
 * contra uma promise que nunca resolve nem rejeita.
 */
function withTimeout<T>(promise: Promise<T>, ms: number, fallback: T): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((resolve) => setTimeout(() => resolve(fallback), ms)),
  ])
}

async function maybeOfferBiometricEnrollment(response: LoginResponse): Promise<void> {
  if (!isWebAuthnSupported() || isBiometricPromptDismissedForever()) {
    goToDestination(response.onboardingCompleted)
    return
  }
  const eligible = await withTimeout(
    Promise.all([
      isPlatformAuthenticatorAvailable(),
      checkStatus(response.deviceId).catch(() => true),
    ]).then(([platformReady, alreadyRegistered]) => platformReady && !alreadyRegistered),
    3_000,
    false,
  )
  if (!eligible) {
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
      // Sem erro de campo específico, o backend não distingue e-mail de
      // senha errados de propósito — então marcamos os dois (docs/telas/02).
      credentialsInvalid.value = Object.keys(normalizedError.fieldErrors).length === 0
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
  credentialsInvalid.value = false
}

function submit(): void {
  if (loginMutation.isPending.value) return
  clearErrors()

  if (!form.identifier.trim()) fieldErrors.identifier = 'Informe seu e-mail ou usuário.'
  if (!form.password) fieldErrors.password = 'Informe sua senha.'
  if (Object.keys(fieldErrors).length > 0) return

  loginMutation.mutate({
    data: {
      identifier: form.identifier.trim(),
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

    <template v-if="biometricAvailable && isMobile">
      <BaseButton
        class="auth-form__biometric"
        variant="secondary"
        :loading="biometricPending"
        @click="submitBiometric"
      >
        Entrar com biometria
      </BaseButton>
      <button
        v-if="!showPasswordForm"
        type="button"
        class="auth-view__use-password"
        @click="showPasswordForm = true"
      >
        Usar e-mail e senha
      </button>
      <p v-if="showPasswordForm" class="auth-view__divider" role="separator">ou entre com sua senha</p>
    </template>

    <form
      v-if="!biometricAvailable || !isMobile || showPasswordForm"
      class="auth-form"
      novalidate
      @submit.prevent="submit"
    >
      <BaseInput
        id="login-identifier"
        v-model="form.identifier"
        label="E-mail ou usuário"
        autocomplete="username"
        placeholder="seu@email.com"
        :error="fieldErrors.identifier"
        :invalid="credentialsInvalid"
        :disabled="loginMutation.isPending.value"
        required
      >
        <template #lead>
          <AppIcon name="envelope" :size="20" />
        </template>
      </BaseInput>
      <PasswordInput
        id="login-password"
        v-model="form.password"
        label="Senha"
        autocomplete="current-password"
        placeholder="Digite sua senha"
        :error="fieldErrors.password"
        :invalid="credentialsInvalid"
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
      <RouterLink class="auth-form__forgot" to="/recuperar-senha">Esqueci minha senha</RouterLink>
    </form>

    <details v-if="mocksEnabled" class="mock-credentials">
      <summary>Credenciais para testar sem backend</summary>
      <p>
        E-mail: <code>{{ mockCredentials.user.email }}</code
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
        <BaseButton variant="ghost" @click="dismissBiometricPromptForever">
          Não mostrar mais ao login
        </BaseButton>
        <BaseButton variant="ghost" @click="biometricPromptOpen = false">Não</BaseButton>
        <BaseButton
          variant="primary"
          :loading="enrollingBiometricAfterLogin"
          @click="confirmBiometricEnrollment"
        >
          Sim
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

.auth-view__use-password {
  display: block;
  width: 100%;
  min-height: var(--control-min-size);
  color: var(--color-primary);
  font: inherit;
  font-weight: var(--font-weight-semibold);
  text-align: center;
  background: none;
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.auth-view__use-password:hover {
  text-decoration: underline;
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

.auth-form__forgot {
  display: block;
  width: 100%;
  min-height: var(--control-min-size);
  margin-block-start: var(--space-1);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  text-align: center;
  text-decoration: none;
}

.auth-form__forgot:hover {
  text-decoration: underline;
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
