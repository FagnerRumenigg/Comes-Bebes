<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useRegister } from '@/api/generated/authentication/authentication'
import type { CreateUserRequest } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'

const router = useRouter()
const form = reactive<CreateUserRequest & { confirmPassword: string }>({
  displayName: '',
  username: '',
  password: '',
  confirmPassword: '',
})
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)
const passwordLengthValid = computed(() => form.password.length >= 8 && form.password.length <= 72)

const registerMutation = useRegister({
  mutation: {
    onSuccess(response) {
      void router.push({
        name: 'login',
        query: { registered: 'true', username: response.username },
      })
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

function validate(): boolean {
  const displayName = form.displayName.trim()
  const username = form.username.trim()
  if (!displayName) {
    fieldErrors.displayName = 'Informe seu nome de exibição.'
  } else if (displayName.length > 100) {
    fieldErrors.displayName = 'Use no máximo 100 caracteres.'
  }
  if (!username) {
    fieldErrors.username = 'Informe um nome de usuário.'
  } else if (!/^[a-zA-Z0-9_]{3,30}$/.test(username)) {
    fieldErrors.username = 'Use de 3 a 30 letras, números ou underscore.'
  }
  if (form.password.length < 8 || form.password.length > 72) {
    fieldErrors.password = 'A senha deve ter entre 8 e 72 caracteres.'
  }
  if (form.confirmPassword !== form.password) {
    fieldErrors.confirmPassword = 'As senhas precisam ser iguais.'
  }
  return Object.keys(fieldErrors).length === 0
}

function submit(): void {
  if (registerMutation.isPending.value) return
  clearErrors()
  if (!validate()) return

  registerMutation.mutate({
    data: {
      displayName: form.displayName.trim(),
      username: form.username.trim(),
      password: form.password,
    },
  })
}
</script>

<template>
  <section class="auth-view" aria-labelledby="register-title">
    <RouterLink class="auth-view__back" to="/login">← Voltar para entrar</RouterLink>

    <div class="auth-view__heading">
      <p class="auth-view__eyebrow">Sua cozinha começa aqui</p>
      <h1 id="register-title">Crie sua conta</h1>
      <p>Escolha como você será encontrado pela comunidade.</p>
    </div>

    <BaseToast
      v-if="generalError"
      title="Não foi possível criar sua conta"
      kind="error"
      @dismiss="generalError = null"
    >
      {{ generalError }}
    </BaseToast>

    <form class="auth-form" novalidate @submit.prevent="submit">
      <BaseInput
        id="register-display-name"
        v-model="form.displayName"
        label="Nome de exibição"
        autocomplete="name"
        placeholder="Como devemos chamar você?"
        hint="Você poderá alterar este nome depois."
        maxlength="100"
        :error="fieldErrors.displayName"
        :disabled="registerMutation.isPending.value"
        required
      />
      <BaseInput
        id="register-username"
        v-model="form.username"
        label="Nome de usuário"
        autocomplete="username"
        placeholder="Ex.: maria_cozinha"
        hint="Use de 3 a 30 letras, números ou underscore."
        :error="fieldErrors.username"
        :disabled="registerMutation.isPending.value"
        required
      />
      <PasswordInput
        id="register-password"
        v-model="form.password"
        label="Senha"
        autocomplete="new-password"
        placeholder="Crie uma senha"
        maxlength="72"
        :error="fieldErrors.password"
        :disabled="registerMutation.isPending.value"
        required
      />
      <p
        class="password-requirement"
        :class="{ 'password-requirement--valid': passwordLengthValid }"
        aria-live="polite"
      >
        <span aria-hidden="true">{{ passwordLengthValid ? '✓' : '○' }}</span>
        Entre 8 e 72 caracteres
      </p>
      <PasswordInput
        id="register-confirm-password"
        v-model="form.confirmPassword"
        label="Confirmar senha"
        autocomplete="new-password"
        placeholder="Digite a senha novamente"
        maxlength="72"
        :error="fieldErrors.confirmPassword"
        :disabled="registerMutation.isPending.value"
        required
      />
      <BaseButton
        class="auth-form__submit"
        type="submit"
        :loading="registerMutation.isPending.value"
      >
        Criar conta
      </BaseButton>
    </form>

    <p class="auth-view__alternative">
      Já tem uma conta?
      <RouterLink to="/login">Entrar agora</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.auth-view {
  display: grid;
  gap: var(--space-6);
  padding-block: var(--space-6);
}

.auth-view__back {
  width: fit-content;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.auth-view__back:hover {
  color: var(--color-primary);
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

.password-requirement {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-start: calc(var(--space-3) * -1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.password-requirement--valid {
  color: var(--color-success);
}

.auth-view__alternative {
  color: var(--color-text-secondary);
  text-align: center;
}

.auth-view__alternative a {
  font-weight: var(--font-weight-semibold);
}
</style>
