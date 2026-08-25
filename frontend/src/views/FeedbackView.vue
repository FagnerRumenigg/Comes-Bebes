<script setup lang="ts">
import { ref } from 'vue'

import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import { useSubmitFeedback } from '@/features/feedback/feedback'

const message = ref('')
const contactEmail = ref('')
const messageError = ref('')
const submitError = ref('')
const sent = ref(false)

const submitMutation = useSubmitFeedback()

function submit(): void {
  if (submitMutation.isPending.value) return
  messageError.value = ''
  submitError.value = ''

  if (!message.value.trim()) {
    messageError.value = 'Escreva sua mensagem antes de enviar.'
    return
  }

  submitMutation.mutate(
    { message: message.value.trim(), contactEmail: contactEmail.value.trim() || undefined },
    {
      onSuccess: () => {
        sent.value = true
      },
      onError: (error) => {
        submitError.value = normalizeHttpError(error).message
      },
    },
  )
}

function sendAnother(): void {
  sent.value = false
  message.value = ''
  contactEmail.value = ''
}
</script>

<template>
  <section class="feedback-view">
    <header class="feedback-view__header">
      <h1>Falar com a gente</h1>
      <p>Achou um problema ou tem uma ideia? Conta pra gente.</p>
    </header>

    <div v-if="sent" class="feedback-view__sent">
      <p>Sua mensagem chegou até nós. Obrigado por escrever!</p>
      <BaseButton variant="secondary" @click="sendAnother">Enviar outra mensagem</BaseButton>
    </div>

    <form v-else class="feedback-view__form" novalidate @submit.prevent="submit">
      <BaseTextarea v-model="message" label="Sua mensagem" required :error="messageError" :rows="6" />
      <BaseInput
        v-model="contactEmail"
        type="email"
        label="E-mail para resposta (opcional)"
        hint="Deixe em branco se quiser usar o e-mail da sua conta."
      />
      <BaseFieldError v-if="submitError" :message="submitError" />
      <BaseButton type="submit" :loading="submitMutation.isPending.value">Enviar</BaseButton>
    </form>
  </section>
</template>

<style scoped>
.feedback-view {
  max-width: var(--content-narrow);
  margin-inline: auto;
}

.feedback-view__header {
  margin-block-end: var(--space-6);
}

.feedback-view__header h1 {
  margin: 0;
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  font-size: clamp(2rem, 5vw, 2.5rem);
}

.feedback-view__header p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
}

.feedback-view__form {
  display: grid;
  gap: var(--space-4);
  justify-items: start;
}

.feedback-view__sent {
  display: grid;
  gap: var(--space-4);
  justify-items: start;
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
