<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useRegister } from '@/api/generated/authentication/authentication'
import type { CreateUserRequest } from '@/api/generated/models'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCheckbox from '@/components/base/BaseCheckbox.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import StatusRing from '@/components/base/StatusRing.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'
import { WELCOME_SEEN_STORAGE_KEY } from '@/utils/storageKeys'

const router = useRouter()
const form = reactive<CreateUserRequest & { confirmPassword: string }>({
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
})
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)
const registeredEmail = ref<string | null>(null)
const termsAccepted = ref(false)
const emailAlreadyExists = ref(false)
const passwordLengthValid = computed(() => form.password.length >= 8 && form.password.length <= 72)

const registerMutation = useRegister({
  mutation: {
    onSuccess(response) {
      registeredEmail.value = response.email
    },
    onError(error) {
      const normalizedError = normalizeHttpError(error)
      Object.assign(fieldErrors, normalizedError.fieldErrors)
      emailAlreadyExists.value = normalizedError.code === 'EMAIL_ALREADY_EXISTS'
      generalError.value = emailAlreadyExists.value ? null : normalizedError.message
    },
  },
})

function clearErrors(): void {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  generalError.value = null
  emailAlreadyExists.value = false
}

function validate(): boolean {
  const displayName = form.displayName.trim()
  const email = form.email.trim()
  if (!displayName) {
    fieldErrors.displayName = 'Informe seu nome de exibição.'
  } else if (displayName.length > 100) {
    fieldErrors.displayName = 'Use no máximo 100 caracteres.'
  }
  if (!email) {
    fieldErrors.email = 'Informe seu e-mail.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    fieldErrors.email = 'Informe um e-mail válido.'
  }
  if (form.password.length < 8 || form.password.length > 72) {
    fieldErrors.password = 'A senha deve ter entre 8 e 72 caracteres.'
  }
  if (form.confirmPassword !== form.password) {
    fieldErrors.confirmPassword = 'As senhas precisam ser iguais.'
  }
  if (!termsAccepted.value) {
    fieldErrors.terms = 'Para criar sua conta, você precisa aceitar os termos.'
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
      email: form.email.trim(),
      password: form.password,
    },
  })
}

function goToFeed(): void {
  // Quem acabou de criar conta já se engajou o bastante — não faz sentido
  // mostrar a tela de boas-vindas de visitante logo em seguida.
  try {
    window.localStorage.setItem(WELCOME_SEEN_STORAGE_KEY, 'true')
  } catch {
    // Sem storage disponível, tudo bem — a pior consequência é ver a
    // tela de boas-vindas uma vez a mais.
  }
  void router.push('/')
}
</script>

<template>
  <section v-if="registeredEmail" class="auth-view auth-view--done" aria-labelledby="done-title">
    <StatusRing variant="success" />
    <h1 id="done-title">Pronto, {{ form.displayName.trim() }}!</h1>
    <p class="auth-view__done-text">
      Sua conta está criada. Enviamos um e-mail para <strong>{{ registeredEmail }}</strong> —
      confirme quando quiser publicar sua primeira receita.
    </p>
    <BaseButton class="auth-form__submit" @click="goToFeed">Começar a explorar</BaseButton>
    <p class="auth-view__done-note">
      Você já pode navegar. A confirmação só é necessária para publicar.
    </p>
  </section>

  <section v-else class="auth-view" aria-labelledby="register-title">
    <RouterLink class="auth-view__back" to="/login">
      <AppIcon name="back" :size="18" :stroke-width="2" />
      Já tenho uma conta
    </RouterLink>

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
        label="Como você quer ser chamado?"
        autocomplete="name"
        placeholder="Ex.: Maria"
        hint="Esse é o nome que outras pessoas verão nas suas publicações."
        maxlength="100"
        :error="fieldErrors.displayName"
        :disabled="registerMutation.isPending.value"
        required
      >
        <template #lead>
          <AppIcon name="person" :size="20" />
        </template>
      </BaseInput>
      <BaseInput
        id="register-email"
        v-model="form.email"
        label="E-mail"
        type="email"
        autocomplete="email"
        placeholder="seu@email.com"
        :hint="emailAlreadyExists ? undefined : 'Você vai usar este e-mail para entrar na sua conta.'"
        :error="fieldErrors.email"
        :invalid="emailAlreadyExists"
        :disabled="registerMutation.isPending.value"
        required
      >
        <template #lead>
          <AppIcon name="envelope" :size="20" />
        </template>
      </BaseInput>
      <p v-if="emailAlreadyExists" class="auth-form__inline-error" role="alert">
        Este e-mail já tem uma conta.
        <RouterLink :to="{ path: '/login', query: { email: form.email.trim() } }">
          Entrar com este e-mail
        </RouterLink>
      </p>
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
        <AppIcon
          :name="passwordLengthValid ? 'check' : 'alert'"
          :size="16"
          :stroke-width="2.4"
          aria-hidden="true"
        />
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
      <BaseCheckbox
        id="register-terms"
        v-model="termsAccepted"
        class="auth-form__terms"
        label="Aceito os termos"
        :error="fieldErrors.terms"
        :disabled="registerMutation.isPending.value"
        required
      >
        Eu aceito os <a href="#">Termos de Serviço</a> e a
        <a href="#">Política de Privacidade</a> do Comes&amp;Bebes.
      </BaseCheckbox>
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

.auth-view--done {
  text-align: center;
}

.auth-view--done h1 {
  margin-block-start: var(--space-2);
}

.auth-view__done-text {
  margin-block-end: var(--space-2);
  color: var(--color-text-secondary);
}

.auth-view__done-note {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.auth-view__back {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: var(--space-2);
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

.auth-form__terms :deep(a) {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.auth-form__inline-error {
  margin-block-start: calc(var(--space-3) * -1);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}

.auth-form__inline-error a {
  color: inherit;
  font-weight: var(--font-weight-semibold);
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
