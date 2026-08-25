<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { apiRequest } from '@/api/client'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import StatusRing from '@/components/base/StatusRing.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'

// POST /auth/password-reset e /auth/password-reset/confirm ainda não
// passaram pelo orval (precisa do backend rodando pra reexportar
// openapi.json) — chamando direto até rodar `npm run api:generate`.

type Step = 'pedir' | 'enviado' | 'nova' | 'pronto' | 'expirado'

const RESEND_COOLDOWN_SECONDS = 60

const route = useRoute()
const router = useRouter()
const routeToken = computed(() => (route.params.token as string | undefined) ?? '')
const step = ref<Step>(routeToken.value ? 'nova' : 'pedir')

// ---------- passo 1: pedir link ----------
const email = ref('')
const requestError = ref('')
const isRequesting = ref(false)

async function requestLink(): Promise<void> {
  if (isRequesting.value) return
  requestError.value = ''
  if (!email.value.trim()) {
    requestError.value = 'Informe seu e-mail.'
    return
  }
  isRequesting.value = true
  try {
    await apiRequest<void>({
      url: '/auth/password-reset',
      method: 'POST',
      data: { email: email.value.trim() },
    })
    step.value = 'enviado'
    startCooldown()
  } catch (error) {
    requestError.value = normalizeHttpError(error).message
  } finally {
    isRequesting.value = false
  }
}

// ---------- passo 2: link enviado ----------
const cooldownSeconds = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | undefined

function startCooldown(): void {
  cooldownSeconds.value = RESEND_COOLDOWN_SECONDS
  clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldownSeconds.value -= 1
    if (cooldownSeconds.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

async function resendLink(): Promise<void> {
  if (cooldownSeconds.value > 0 || isRequesting.value) return
  isRequesting.value = true
  try {
    await apiRequest<void>({
      url: '/auth/password-reset',
      method: 'POST',
      data: { email: email.value.trim() },
    })
  } finally {
    isRequesting.value = false
    startCooldown()
  }
}

function useAnotherEmail(): void {
  clearInterval(cooldownTimer)
  cooldownSeconds.value = 0
  step.value = 'pedir'
}

// ---------- passo 3: nova senha ----------
const newPassword = ref('')
const confirmPassword = ref('')
const newPasswordError = ref('')
const confirmPasswordError = ref('')
const isConfirming = ref(false)
const passwordLengthValid = computed(
  () => newPassword.value.length >= 8 && newPassword.value.length <= 72,
)

async function confirmNewPassword(): Promise<void> {
  if (isConfirming.value) return
  newPasswordError.value = ''
  confirmPasswordError.value = ''
  if (!passwordLengthValid.value) {
    newPasswordError.value = 'A senha deve ter entre 8 e 72 caracteres.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    confirmPasswordError.value = 'As senhas precisam ser iguais.'
    return
  }
  isConfirming.value = true
  try {
    await apiRequest<void>({
      url: '/auth/password-reset/confirm',
      method: 'POST',
      data: { token: routeToken.value, newPassword: newPassword.value },
    })
    step.value = 'pronto'
  } catch (error) {
    const normalized = normalizeHttpError(error)
    if (normalized.code === 'PASSWORD_RESET_TOKEN_INVALID') {
      step.value = 'expirado'
    } else {
      newPasswordError.value = normalized.message
    }
  } finally {
    isConfirming.value = false
  }
}

// ---------- link expirado / pronto ----------
function requestNewLink(): void {
  step.value = 'pedir'
}

function goToLogin(): void {
  void router.push({ path: '/login', query: email.value.trim() ? { email: email.value.trim() } : {} })
}

onBeforeUnmount(() => clearInterval(cooldownTimer))
</script>

<template>
  <section class="forgot-password-view">
    <!-- 1 — pedir link -->
    <template v-if="step === 'pedir'">
      <RouterLink class="forgot-password-view__back" to="/login">
        <AppIcon name="back" :size="18" :stroke-width="2.2" />
        Voltar para entrar
      </RouterLink>
      <h1>Esqueceu a senha?</h1>
      <p class="forgot-password-view__lead">
        Acontece. Digite seu e-mail e mandamos um link para você criar uma nova.
      </p>
      <form class="forgot-password-view__form" novalidate @submit.prevent="requestLink">
        <BaseInput
          id="forgot-password-email"
          v-model="email"
          type="email"
          label="E-mail"
          autocomplete="email"
          placeholder="Digite seu e-mail"
          hint="O mesmo que você usa para entrar."
          :disabled="isRequesting"
          required
        >
          <template #lead>
            <AppIcon name="envelope" :size="20" />
          </template>
        </BaseInput>
        <BaseFieldError v-if="requestError" :message="requestError" />
        <BaseButton type="submit" class="forgot-password-view__submit" :loading="isRequesting">
          Enviar o link
        </BaseButton>
      </form>
    </template>

    <!-- 2 — link enviado -->
    <template v-else-if="step === 'enviado'">
      <div class="forgot-password-view__center">
        <AppIcon class="forgot-password-view__art" name="envelope" :size="80" :stroke-width="1.3" />
        <h1>Olhe seu e-mail</h1>
        <p class="forgot-password-view__lead">
          Se existir uma conta com <strong>{{ email }}</strong
          >, o link já está a caminho. Ele vale por 1 hora.
        </p>
      </div>
      <p class="forgot-password-view__tip">
        Não achou? Veja na caixa de spam ou na aba de promoções. O remetente é Comes&amp;Bebes.
      </p>
      <BaseButton
        variant="secondary"
        class="forgot-password-view__submit"
        :disabled="cooldownSeconds > 0"
        :loading="isRequesting"
        @click="resendLink"
      >
        {{ cooldownSeconds > 0 ? `Reenviar em ${cooldownSeconds}s` : 'Reenviar o link' }}
      </BaseButton>
      <button type="button" class="forgot-password-view__text-link" @click="useAnotherEmail">
        Usar outro e-mail
      </button>
    </template>

    <!-- 3 — nova senha -->
    <template v-else-if="step === 'nova'">
      <h1>Crie uma senha nova</h1>
      <p class="forgot-password-view__lead">Escolha algo que você lembre. Pode ser uma frase.</p>
      <form class="forgot-password-view__form" novalidate @submit.prevent="confirmNewPassword">
        <PasswordInput
          id="forgot-password-new"
          v-model="newPassword"
          label="Senha nova"
          autocomplete="new-password"
          placeholder="Crie uma senha"
          :error="newPasswordError"
          :disabled="isConfirming"
          required
        />
        <p class="forgot-password-view__rule" :class="{ 'forgot-password-view__rule--ok': passwordLengthValid }">
          <AppIcon
            :name="passwordLengthValid ? 'check' : 'alert'"
            :size="16"
            :stroke-width="2.4"
            aria-hidden="true"
          />
          Entre 8 e 72 caracteres.
        </p>
        <PasswordInput
          id="forgot-password-confirm"
          v-model="confirmPassword"
          label="Repita a senha nova"
          autocomplete="new-password"
          placeholder="Digite a senha novamente"
          :error="confirmPasswordError"
          :disabled="isConfirming"
          required
        />
        <p class="forgot-password-view__tip">
          Ao salvar, sua conta sai de todos os aparelhos. Você entra de novo com a senha nova.
        </p>
        <BaseButton type="submit" class="forgot-password-view__submit" :loading="isConfirming">
          Salvar a senha nova
        </BaseButton>
      </form>
    </template>

    <!-- 4 — pronto -->
    <template v-else-if="step === 'pronto'">
      <div class="forgot-password-view__center">
        <StatusRing variant="success" />
        <h1>Senha trocada</h1>
        <p class="forgot-password-view__lead">Pronto. Agora é só entrar com a senha nova.</p>
      </div>
      <BaseButton class="forgot-password-view__submit" @click="goToLogin">Entrar</BaseButton>
    </template>

    <!-- link expirado -->
    <template v-else>
      <div class="forgot-password-view__center">
        <span class="forgot-password-view__ring forgot-password-view__ring--warning">
          <AppIcon name="clock" :size="34" :stroke-width="2" />
        </span>
        <h1>Este link não vale mais</h1>
        <p class="forgot-password-view__lead">
          Os links valem 1 hora, por segurança. Peça um novo que a gente manda na hora.
        </p>
      </div>
      <BaseButton class="forgot-password-view__submit" @click="requestNewLink">
        Pedir um link novo
      </BaseButton>
      <RouterLink class="forgot-password-view__text-link" to="/login">Voltar para entrar</RouterLink>
    </template>
  </section>
</template>

<style scoped>
.forgot-password-view {
  max-width: 26rem;
  margin-inline: auto;
  padding-block: var(--space-8);
}

.forgot-password-view__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-end: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.forgot-password-view__back:hover {
  color: var(--color-primary);
}

.forgot-password-view h1 {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-3xl);
}

.forgot-password-view__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.forgot-password-view__lead strong {
  color: var(--color-text);
}

.forgot-password-view__form {
  display: grid;
  gap: var(--space-5);
}

.forgot-password-view__submit {
  width: 100%;
}

.forgot-password-view__center {
  text-align: center;
}

.forgot-password-view__center .forgot-password-view__lead {
  max-width: 28em;
  margin-inline: auto;
}

.forgot-password-view__art {
  display: block;
  margin: 0 auto var(--space-5);
  color: var(--color-primary);
}

.forgot-password-view__ring {
  display: grid;
  width: 4.875rem;
  height: 4.875rem;
  margin: 0 auto var(--space-5);
  place-items: center;
  border-radius: var(--radius-pill);
}

.forgot-password-view__ring--warning {
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 16%, var(--color-surface));
  border: 2px solid var(--color-danger);
}

.forgot-password-view__tip {
  padding: var(--space-3) var(--space-4);
  margin-block-end: var(--space-5);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.forgot-password-view__rule {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-start: calc(var(--space-3) * -1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.forgot-password-view__rule--ok {
  color: var(--color-success);
}

.forgot-password-view__text-link {
  display: block;
  width: 100%;
  min-height: var(--control-min-size);
  margin-block-start: var(--space-2);
  color: var(--color-primary);
  font: inherit;
  font-weight: var(--font-weight-semibold);
  text-align: center;
  text-decoration: none;
  background: none;
  border: 0;
  cursor: pointer;
}

.forgot-password-view__text-link:hover {
  text-decoration: underline;
}
</style>
