<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { normalizeHttpError } from '@/api/errors'
import { useCompleteOnboarding } from '@/api/generated/users/users'
import BrandMark from '@/components/layout/BrandMark.vue'
import SheetPage from '@/components/layout/SheetPage.vue'
import { useAuthStore } from '@/stores/auth.store'
import { WELCOME_SEEN_STORAGE_KEY } from '@/utils/storageKeys'

const router = useRouter()
const authStore = useAuthStore()
const errorMessage = ref<string | null>(null)

// Boas-vindas serve tanto para visitante de primeira vez (docs/telas/01)
// quanto para conta recém-criada que ainda não concluiu o onboarding -
// são a mesma tela, não duas telas diferentes.
const completeOnboardingMutation = useCompleteOnboarding({
  mutation: {
    onSuccess: () => {
      authStore.markOnboardingCompleted()
      void router.push('/')
    },
    onError: (error) => {
      errorMessage.value = normalizeHttpError(error).message
    },
  },
})

function start(): void {
  if (authStore.authenticated && authStore.identity?.onboardingCompleted === false) {
    if (completeOnboardingMutation.isPending.value) return
    errorMessage.value = null
    completeOnboardingMutation.mutate({ id: authStore.identity.userId })
    return
  }

  try {
    window.localStorage.setItem(WELCOME_SEEN_STORAGE_KEY, 'true')
  } catch {
    // Sem storage disponível: só não insiste em mostrar de novo nesta navegação.
  }
  void router.push('/')
}
</script>

<template>
  <SheetPage>
    <section aria-labelledby="welcome-title">
      <div class="welcome-view__brand">
        <BrandMark size="medium" />
        <span>Comes&amp;Bebes</span>
      </div>

      <h1 id="welcome-title">Senta que <em>já vai sair</em></h1>
      <p class="welcome-view__lead">
        Este é um lugar para compartilhar aquilo de que todo mundo gosta: comida boa.
      </p>

      <div class="welcome-view__cards">
        <div class="welcome-view__card">
          <span class="welcome-view__card-icon" aria-hidden="true">🍳</span>
          <div>
            <h2>Publique o que você cozinha</h2>
            <p>
              Seus pratos, suas receitas, sua versão daquela receita que você viu por aqui. Bonito,
              torto ou de terça-feira — tudo cabe.
            </p>
          </div>
        </div>
        <div class="welcome-view__card">
          <span class="welcome-view__card-icon" aria-hidden="true">🌿</span>
          <div>
            <h2>Sem comentário, sem plateia</h2>
            <p>
              Não tem comentário, não tem foto de gente, não tem quem ganha. Gostou de alguma
              coisa? Reaja, salve, ou faça a sua versão.
            </p>
          </div>
        </div>
        <div class="welcome-view__card">
          <span class="welcome-view__card-icon" aria-hidden="true">🍋</span>
          <div>
            <h2>Simples de propósito</h2>
            <p>Comida, receitas e inspiração. Só isso. E a intenção é continuar assim.</p>
          </div>
        </div>
      </div>

      <p class="welcome-view__note">
        O Comes&amp;Bebes é novo e ainda está crescendo. Se alguma coisa parecer estranha, ou se
        você tiver uma ideia, <strong>conta pra gente</strong> — a cozinha ainda está sendo
        montada.
      </p>

      <button
        type="button"
        class="welcome-view__submit"
        :disabled="completeOnboardingMutation.isPending.value"
        @click="start"
      >
        {{ completeOnboardingMutation.isPending.value ? 'Só um instante…' : 'Começar a explorar' }}
      </button>
      <p v-if="errorMessage" class="welcome-view__error" role="alert">{{ errorMessage }}</p>
    </section>
  </SheetPage>
</template>

<style scoped>
.welcome-view__brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-block-end: var(--space-6);
  font-family: var(--font-editorial);
  font-size: var(--font-size-xl);
}

h1 {
  margin-bottom: var(--space-3);
  font-family: var(--font-editorial);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-regular);
  letter-spacing: var(--letter-spacing-tight);
  line-height: var(--line-height-tight);
}

h1 em {
  color: var(--color-primary);
  font-style: italic;
}

.welcome-view__lead {
  max-width: 34em;
  margin-bottom: var(--space-7);
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
}

.welcome-view__cards {
  display: grid;
  gap: var(--space-3);
  margin-bottom: var(--space-7);
}

.welcome-view__card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.welcome-view__card-icon {
  display: grid;
  width: 2.5rem;
  height: 2.5rem;
  flex: none;
  font-size: var(--font-size-xl);
  line-height: 1;
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
  border-radius: var(--radius-md);
  place-items: center;
}

.welcome-view__card h2 {
  margin-bottom: 0.1875rem;
  font-family: var(--font-interface);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  letter-spacing: normal;
}

.welcome-view__card p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
}

.welcome-view__note {
  padding-inline-start: var(--space-4);
  margin-bottom: var(--space-7);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-body);
  border-inline-start: 3px solid var(--color-accent);
}

.welcome-view__note strong {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.welcome-view__submit {
  display: flex;
  width: 100%;
  min-height: 3.25rem;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-contrast);
  font: inherit;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.welcome-view__submit:hover:not(:disabled) {
  filter: brightness(1.08);
}

.welcome-view__submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.welcome-view__error {
  margin-top: var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  text-align: center;
}
</style>
