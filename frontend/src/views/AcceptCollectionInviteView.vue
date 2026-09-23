<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { acceptCollectionInvite } from '@/api/generated/collections/collections'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const errorMessage = ref('')
const accepting = ref(false)
const accepted = ref(false)
const token = String(route.params.token ?? '')

function inviteError(error: unknown): string {
  const normalized = normalizeHttpError(error)
  const code = normalized.code?.toUpperCase() ?? ''
  const message = normalized.message.toLowerCase()

  if (code.includes('EXPIRED') || message.includes('expir')) {
    return 'Este convite expirou. Peça à pessoa responsável pela coleção um novo convite.'
  }
  if (code.includes('USED') || code.includes('ACCEPTED') || message.includes('utiliz')) {
    return 'Este convite já foi utilizado. Peça um novo convite se precisar entrar na coleção.'
  }
  return normalized.message || 'Este convite não existe ou não está mais disponível.'
}

async function acceptInvite(): Promise<void> {
  if (accepting.value || accepted.value) return
  if (!authStore.authenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  accepting.value = true
  errorMessage.value = ''
  try {
    const collection = await acceptCollectionInvite(token)
    accepted.value = true
    await router.replace(`/colecoes/${collection.id}`)
  } catch (error) {
    errorMessage.value = inviteError(error)
  } finally {
    accepting.value = false
  }
}
</script>

<template>
  <section class="accept-invite">
    <div v-if="errorMessage" class="accept-invite__state" role="alert">
      <strong>Não foi possível aceitar este convite.</strong>
      <p>{{ errorMessage }}</p>
      <BaseButton variant="secondary" @click="router.push('/')">Voltar ao início</BaseButton>
    </div>
    <div v-else class="accept-invite__state">
      <p class="accept-invite__eyebrow">Convite para coleção</p>
      <h1>Você foi convidado para colaborar</h1>
      <p>
        Ao aceitar, você poderá acessar a coleção compartilhada e contribuir com novas referências de comida.
      </p>
      <p class="accept-invite__privacy">
        Você pode recusar sem qualquer penalidade. O convite só será aceito quando você confirmar abaixo.
      </p>
      <BaseButton :loading="accepting" :disabled="accepted" @click="acceptInvite">
        Aceitar convite
      </BaseButton>
      <BaseButton variant="ghost" :disabled="accepting" @click="router.push('/')">
        Recusar e voltar
      </BaseButton>
    </div>
  </section>
</template>

<style scoped>
.accept-invite {
  display: grid;
  place-content: center;
  min-height: 40vh;
}
.accept-invite__state {
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.accept-invite__state h1 {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-2xl);
}
.accept-invite__state p {
  max-width: 32rem;
  margin: 0;
}
.accept-invite__eyebrow {
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.accept-invite__privacy {
  font-size: var(--font-size-sm);
}
</style>
