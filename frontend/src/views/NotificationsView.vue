<script setup lang="ts">
import { computed } from 'vue'

import { useNotifications } from '@/api/generated/users/users'
import { useAuthStore } from '@/stores/auth.store'

const authStore = useAuthStore()
const userId = computed(() => authStore.identity?.userId ?? '')
const notificationsQuery = useNotifications(userId, { page: 1, size: 20 })
const labels: Record<string, string> = {
  PUBLICATION_APPROVED: 'Sua publicação foi aprovada.',
  PUBLICATION_REPORTED: 'Uma publicação foi denunciada.',
  REPORT_REJECTED_WARNING: 'Sua denúncia foi analisada e a publicação foi mantida.',
  NEW_DEVICE_LOGIN: 'Novo login detectado em um dispositivo. Não foi você? Revogue o acesso.',
  NEW_FOLLOWER: 'Alguém começou a seguir você.',
  FOLLOWED_USER_PUBLISHED: 'Alguém que você segue publicou algo novo.',
}
</script>

<template>
  <section class="notifications-view">
    <header class="notifications-view__header">
      <p class="notifications-view__eyebrow">Atualizações</p>
      <h1>Notificações</h1>
      <p>Acompanhe os avisos relacionados às suas publicações.</p>
    </header>
    <div v-if="notificationsQuery.isPending.value" class="notifications-view__state">
      Carregando notificações...
    </div>
    <div
      v-else-if="notificationsQuery.isError.value"
      class="notifications-view__state"
      role="alert"
    >
      Não foi possível carregar suas notificações.
    </div>
    <div
      v-else-if="!notificationsQuery.data.value?.content.length"
      class="notifications-view__state"
    >
      Você não tem notificações novas.
    </div>
    <ul v-else class="notifications-view__list">
      <li
        v-for="notification in notificationsQuery.data.value.content"
        :key="notification.id"
        :class="{ 'notifications-view__item--unread': !notification.readAt }"
      >
        <strong>{{ labels[notification.type] ?? 'Você recebeu uma nova notificação.' }}</strong
        ><span>{{ notification.readAt ? 'Lida' : 'Não lida' }}</span
        ><RouterLink v-if="notification.publicationId" :to="`/publicacoes/${notification.publicationId}`"
          >Ver publicação</RouterLink
        ><RouterLink v-else-if="notification.type === 'NEW_DEVICE_LOGIN'" to="/dispositivos"
          >Ver dispositivos</RouterLink
        >
      </li>
    </ul>
  </section>
</template>

<style scoped>
.notifications-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.notifications-view__header {
  margin-block-end: var(--space-8);
}
.notifications-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.notifications-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.notifications-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
.notifications-view__list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  list-style: none;
}
.notifications-view__list li {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.notifications-view__item--unread {
  border-inline-start: 4px solid var(--color-primary);
}
.notifications-view__list span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.notifications-view__list a {
  width: fit-content;
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}
.notifications-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
</style>
