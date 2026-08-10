<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'

import BaseButton from '@/components/base/BaseButton.vue'
import { apiRequest } from '@/api/client'
import { normalizeHttpError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const authStore = useAuthStore()
const errorMessage = ref<string | null>(null)

const completeOnboardingMutation = useMutation({
  mutationFn: async (userId: string) =>
    apiRequest<void>({ url: `/users/${userId}/onboarding`, method: 'PATCH' }),
  onSuccess: () => {
    if (authStore.identity) {
      authStore.identity.onboardingCompleted = true
    }
    void router.replace({ name: 'feed' })
  },
  onError: (error) => {
    errorMessage.value = normalizeHttpError(error).message
  },
})

function submit(): void {
  if (!authStore.identity?.userId || completeOnboardingMutation.isPending.value) return
  errorMessage.value = null
  void completeOnboardingMutation.mutateAsync(authStore.identity.userId)
}
</script>

<template>
  <section class="onboarding-view" aria-labelledby="onboarding-title">
    <div class="onboarding-view__content">
      <p class="onboarding-view__eyebrow">Primeiros passos</p>
      <h1 id="onboarding-title">Bem-vindo ao Comes&Bebes</h1>
      <div class="onboarding-view__content-text">
        <p class="onboarding-view__description">
          <strong>Bem-vindo ao Comes&amp;Bebes! 🍜</strong>
          O Comes&amp;Bebes é um espaço para compartilhar aquilo que todo mundo gosta:
          <strong>comida boa.</strong>
        </p>

        <p class="onboarding-view__description">
          Aqui você pode publicar seus pratos, compartilhar receitas, mostrar sua versão de uma
          receita que encontrou por aqui e descobrir o que outras pessoas estão preparando.
        </p>

        <p class="onboarding-view__description">
          A ideia é manter as coisas simples: <strong>comida, receitas e inspiração.</strong>
          Sem comentários, sem fotos de pessoas e sem aquela pressão de outras redes sociais. Gostou
          de alguma coisa? Reaja, salve ou faça a sua própria versão.
        </p>

        <div class="onboarding-view__card">
          <h2>🧪 Estamos em fase de testes</h2>
          <p>
            O Comes&amp;Bebes ainda está sendo desenvolvido e você está participando dessa fase
            inicial.
          </p>
          <p>
            Isso significa que algumas coisas podem mudar, aparecer ou até quebrar de vez em quando.
            Se encontrar algum problema ou tiver uma ideia, seu feedback é muito bem-vindo.
          </p>
        </div>

        <div class="onboarding-view__card onboarding-view__card--secondary">
          <h2>🌙 E o servidor também dorme</h2>
          <p>
            Durante esta fase de testes, o Comes&amp;Bebes ficará disponível
            <strong>das 8h às 23h</strong>.
          </p>
          <p>
            Entre <strong>23h e 8h</strong>, o servidor ficará desligado e algumas funcionalidades
            não estarão disponíveis.
          </p>
          <p>
            Não é bug. O servidor só tem um horário de sono mais saudável que o desenvolvedor. 😴
          </p>
        </div>

        <p class="onboarding-view__description onboarding-view__description--final">
          <strong
            >Agora sim: fique à vontade, publique alguma coisa gostosa e veja o que o pessoal anda
            cozinhando.</strong
          >
        </p>
      </div>

      <BaseButton
        class="onboarding-view__action"
        @click="submit"
        :loading="completeOnboardingMutation.isPending.value"
      >
        Começar a explorar
      </BaseButton>

      <p v-if="errorMessage" class="onboarding-view__error" role="alert">{{ errorMessage }}</p>
    </div>
  </section>
</template>

<style scoped>
.onboarding-view {
  display: grid;
  place-items: center;
  min-height: 70vh;
}

.onboarding-view__content {
  width: min(100%, 42rem);
  display: grid;
  gap: var(--space-5);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.onboarding-view__eyebrow {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.onboarding-view__description {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.onboarding-view__description--final {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.onboarding-view__content-text {
  display: grid;
  gap: var(--space-4);
}

.onboarding-view__card {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface) 70%, var(--color-background));
}

.onboarding-view__card p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.onboarding-view__card--secondary {
  background: color-mix(in srgb, var(--color-surface) 60%, var(--color-background));
}

.onboarding-view__action {
  justify-self: start;
}

.onboarding-view__error {
  color: var(--color-danger);
}
</style>
