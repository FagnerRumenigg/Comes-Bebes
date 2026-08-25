<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUpdateCurrentUser } from '@/api/generated/users/users'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ email: '' })
const fieldErrors = reactive<Record<string, string>>({})
const generalError = ref<string | null>(null)

const updateMutation = useUpdateCurrentUser({
  mutation: {
    onSuccess: () => {
      authStore.markEmailProvided()
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
      void router.replace(safeRedirect)
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

function submit(): void {
  if (!authStore.identity?.userId || updateMutation.isPending.value) return
  clearErrors()
  if (!form.email.trim()) {
    fieldErrors.email = 'Informe seu e-mail.'
    return
  }
  updateMutation.mutate({ id: authStore.identity.userId, data: { email: form.email.trim() } })
}
</script>

<template>
  <section class="require-email" aria-labelledby="require-email-title">
    <div class="require-email__content">
      <p class="require-email__eyebrow">Só mais um passo</p>
      <h1 id="require-email-title">Qual é o seu e-mail?</h1>
      <p class="require-email__description">
        Sua conta foi criada antes de o Comes&amp;Bebes passar a usar e-mail para entrar. Informe o
        seu para continuar — da próxima vez, é por ele que você entra.
      </p>

      <form class="require-email__form" novalidate @submit.prevent="submit">
        <BaseInput
          id="require-email-input"
          v-model="form.email"
          type="email"
          label="E-mail"
          autocomplete="email"
          placeholder="seu@email.com"
          :error="fieldErrors.email"
          :disabled="updateMutation.isPending.value"
          required
        />
        <p v-if="generalError" class="require-email__error" role="alert">{{ generalError }}</p>
        <BaseButton type="submit" :loading="updateMutation.isPending.value">Continuar</BaseButton>
      </form>
    </div>
  </section>
</template>

<style scoped>
.require-email {
  display: grid;
  place-items: center;
  min-height: 70vh;
}

.require-email__content {
  width: min(100%, 28rem);
  display: grid;
  gap: var(--space-5);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.require-email__eyebrow {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.require-email__description {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.require-email__form {
  display: grid;
  gap: var(--space-4);
}

.require-email__error {
  margin: 0;
  color: var(--color-danger);
}
</style>
