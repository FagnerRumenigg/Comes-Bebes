<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { acceptCollectionInvite } from '@/api/generated/collections/collections'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const errorMessage = ref('')

onMounted(async () => {
  const token = String(route.params.token ?? '')
  try {
    const collection = await acceptCollectionInvite(token)
    await router.replace(`/colecoes/${collection.id}`)
  } catch (error) {
    errorMessage.value = normalizeHttpError(error).message || 'Este convite não é válido.'
  }
})
</script>

<template>
  <section class="accept-invite">
    <div v-if="errorMessage" class="accept-invite__state" role="alert">
      <strong>Não foi possível aceitar este convite.</strong>
      <p>{{ errorMessage }}</p>
      <BaseButton variant="secondary" @click="router.push('/')">Voltar ao início</BaseButton>
    </div>
    <div v-else class="accept-invite__state" role="status">Entrando na coleção…</div>
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
</style>
