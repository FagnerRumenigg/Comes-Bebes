<script setup lang="ts">
import { computed } from 'vue'

import AppIcon from '@/components/icons/AppIcon.vue'
import { useAuthStore } from '@/stores/auth.store'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useUnreadNotificationsCount } from '@/composables/useUnreadNotificationsCount'

const authStore = useAuthStore()
const { count: unreadCount } = useUnreadNotificationsCount()
const profileDestination = computed(() => `/u/${authStore.identity?.username ?? ''}`)
</script>

<template>
  <nav class="mobile-navigation" aria-label="Navegação móvel">
    <RouterLink to="/">
      <AppIcon name="home" :size="22" :stroke-width="1.8" />
      <span>Início</span>
    </RouterLink>
    <RouterLink v-if="authStore.authenticated" to="/salvos">
      <AppIcon name="bookmark" :size="22" :stroke-width="1.8" />
      <span>Salvos</span>
    </RouterLink>
    <button v-else type="button" @click="showAuthNotice">
      <AppIcon name="bookmark" :size="22" :stroke-width="1.8" />
      <span>Salvos</span>
    </button>
    <RouterLink v-if="authStore.authenticated" class="mobile-navigation__publish" to="/publicar">
      <span class="mobile-navigation__publish-circle" aria-hidden="true">
        <AppIcon name="plus" :size="23" :stroke-width="2.4" />
      </span>
      <span>Publicar</span>
    </RouterLink>
    <button v-else type="button" class="mobile-navigation__publish" @click="showAuthNotice">
      <span class="mobile-navigation__publish-circle" aria-hidden="true">
        <AppIcon name="plus" :size="23" :stroke-width="2.4" />
      </span>
      <span>Publicar</span>
    </button>
    <RouterLink
      v-if="authStore.authenticated"
      to="/notificacoes"
      :aria-label="unreadCount > 0 ? `Avisos, ${unreadCount} novos` : 'Avisos'"
    >
      <span v-if="unreadCount > 0" class="mobile-navigation__badge" aria-hidden="true">{{
        unreadCount
      }}</span>
      <AppIcon name="bell" :size="22" :stroke-width="1.8" />
      <span>Avisos</span>
    </RouterLink>
    <button v-else type="button" @click="showAuthNotice">
      <AppIcon name="bell" :size="22" :stroke-width="1.8" />
      <span>Avisos</span>
    </button>
    <RouterLink v-if="authStore.authenticated" :to="profileDestination">
      <AppIcon name="person" :size="22" :stroke-width="1.8" />
      <span>Perfil</span>
    </RouterLink>
    <RouterLink v-else to="/login">
      <AppIcon name="person" :size="22" :stroke-width="1.8" />
      <span>Entrar</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.mobile-navigation {
  position: fixed;
  z-index: 20;
  right: 0;
  bottom: 0;
  left: 0;
  display: none;
  min-height: 4rem;
  grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
  align-items: stretch;
  padding: var(--space-1) max(var(--space-1), env(safe-area-inset-right))
    max(var(--space-2), env(safe-area-inset-bottom)) max(var(--space-1), env(safe-area-inset-left));
  background: var(--color-surface-raised);
  border-block-start: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.mobile-navigation a,
.mobile-navigation button {
  position: relative;
  display: flex;
  min-height: 3.5rem;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.1875rem;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-align: center;
  text-decoration: none;
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
}

.mobile-navigation a.router-link-exact-active {
  position: relative;
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}

.mobile-navigation a.router-link-exact-active::before {
  position: absolute;
  top: -0.375rem;
  width: 1.625rem;
  height: 0.1875rem;
  background: var(--color-primary);
  border-radius: var(--radius-pill);
  content: '';
}

.mobile-navigation__publish {
  flex: none !important;
  width: 4.875rem;
}

.mobile-navigation__publish-circle {
  display: grid;
  width: 3.125rem;
  height: 3.125rem;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  place-items: center;
}

.mobile-navigation__publish span:last-child {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.mobile-navigation__badge {
  position: absolute;
  top: 0.25rem;
  right: calc(50% - 1.375rem);
  display: grid;
  min-width: 1.1875rem;
  height: 1.1875rem;
  padding-inline: 0.3125rem;
  color: var(--color-primary-contrast);
  font-size: 0.6875rem;
  font-weight: var(--font-weight-bold);
  line-height: 1;
  background: var(--color-danger);
  border-radius: var(--radius-pill);
  place-items: center;
}

@media (max-width: 48rem) {
  .mobile-navigation {
    display: grid;
  }
}
</style>
