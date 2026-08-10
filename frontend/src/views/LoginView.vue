<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useLogin } from '@/api/generated/authentication/authentication'
import type { LoginRequest } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCheckbox from '@/components/base/BaseCheckbox.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'
import { mockCredentials } from '@/mocks/fixtures/auth'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive<LoginRequest>({ username: '', password: '' })
const remember = ref(false)
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)
const mocksEnabled = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS !== 'false'

if (typeof route.query.username === 'string') form.username = route.query.username

const loginMutation = useLogin({
  mutation: {
    onSuccess(response) {
      authStore.acceptSession(response, remember.value)
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
      const target = response.onboardingCompleted ? safeRedirect : '/onboarding'
      void router.replace(target)
    },
    onError(error) {
      const normalizedError = normalizeHttpError(error)
      Object.assign(fieldErrors, normalizedError.fieldErrors)
      generalError.value = normalizedError.message
    },
  },
})

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
