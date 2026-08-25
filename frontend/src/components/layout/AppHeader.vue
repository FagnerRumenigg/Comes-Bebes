<script setup lang="ts">
import { useRoute } from 'vue-router'

import AppIcon from '@/components/icons/AppIcon.vue'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useUnreadNotificationsCount } from '@/composables/useUnreadNotificationsCount'
import { useFeedRefresh } from '@/features/feed/useFeedRefresh'
import { useAuthStore } from '@/stores/auth.store'

import AccountMenu from './AccountMenu.vue'
import BrandMark from './BrandMark.vue'
import ThemeSwitch from './ThemeSwitch.vue'

const route = useRoute()
const authStore = useAuthStore()
const { refreshFeed } = useFeedRefresh()
const { count: unreadCount } = useUnreadNotificationsCount()

function isActive(path: string): boolean {
  return path === '/' ? route.path === '/' : route.path.startsWith(path)
}

function goHome(): void {
  if (route.path !== '/') return
  void refreshFeed()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <RouterLink
        class="app-header__brand"
        to="/"
        aria-label="Comes&Bebes — início"
        @click="goHome"
      >
        <BrandMark />
        <span>Comes&amp;Bebes</span>
      </RouterLink>

      <nav class="app-header__navigation" aria-label="Navegação principal">
        <RouterLink to="/" :class="{ 'app-header__nav-link--on': isActive('/') }">
          <AppIcon name="home" :size="20" :stroke-width="1.8" />
          Início
        </RouterLink>
        <RouterLink
          v-if="authStore.authenticated"
          to="/salvos"
          :class="{ 'app-header__nav-link--on': isActive('/salvos') }"
        >
          <AppIcon name="bookmark" :size="20" :stroke-width="1.8" />
          Salvos
        </RouterLink>
        <button v-else type="button" @click="showAuthNotice">
          <AppIcon name="bookmark" :size="20" :stroke-width="1.8" />
          Salvos
        </button>
        <RouterLink
          v-if="authStore.authenticated"
          to="/notificacoes"
          :class="{ 'app-header__nav-link--on': isActive('/notificacoes') }"
          :aria-label="unreadCount > 0 ? `Avisos, ${unreadCount} novos` : 'Avisos'"
        >
          <AppIcon name="bell" :size="20" :stroke-width="1.8" />
          Avisos
          <span v-if="unreadCount > 0" class="app-header__badge" aria-hidden="true">{{
            unreadCount
          }}</span>
        </RouterLink>
        <button v-else type="button" @click="showAuthNotice">
          <AppIcon name="bell" :size="20" :stroke-width="1.8" />
          Avisos
        </button>
      </nav>

      <span class="app-header__spacer" />

      <RouterLink v-if="authStore.authenticated" class="app-header__publish" to="/publicar">
        <AppIcon name="plus" :size="20" :stroke-width="2.2" />
        Publicar
      </RouterLink>
      <button v-else type="button" class="app-header__publish" @click="showAuthNotice">
        <AppIcon name="plus" :size="20" :stroke-width="2.2" />
        Publicar
      </button>

      <div class="app-header__actions">
        <ThemeSwitch v-if="!authStore.authenticated" />
        <AccountMenu v-if="authStore.authenticated" />
        <RouterLink v-else class="app-header__session" to="/login">Entrar</RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  z-index: 20;
  top: 0;
  background: color-mix(in srgb, var(--color-background) 92%, transparent);
  border-block-end: 1px solid var(--color-border);
  backdrop-filter: blur(0.75rem);
}

.app-header__inner {
  display: flex;
  width: min(calc(100% - var(--space-8)), var(--content-wide));
  min-height: 4.375rem;
  align-items: center;
  gap: var(--space-6);
  margin-inline: auto;
}

.app-header__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text);
  font-family: var(--font-editorial);
  font-size: var(--font-size-lg);
  text-decoration: none;
}

.app-header__navigation {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.app-header__navigation a,
.app-header__navigation button {
  display: flex;
  min-height: 3rem;
  align-items: center;
  gap: var(--space-2);
  padding-inline: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
}

.app-header__navigation a:hover,
.app-header__navigation button:hover {
  color: var(--color-text);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
}

.app-header__nav-link--on {
  position: relative;
  color: var(--color-text) !important;
  font-weight: var(--font-weight-bold) !important;
}

.app-header__nav-link--on::after {
  position: absolute;
  right: var(--space-3);
  bottom: -0.75rem;
  left: var(--space-3);
  height: 0.1875rem;
  background: var(--color-primary);
  border-radius: var(--radius-pill);
  content: '';
}

.app-header__badge {
  display: grid;
  min-width: 1.3125rem;
  height: 1.3125rem;
  padding-inline: var(--space-1);
  color: var(--color-primary-contrast);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  line-height: 1;
  background: var(--color-danger);
  border-radius: var(--radius-pill);
  place-items: center;
}

.app-header__spacer {
  flex: 1;
}

.app-header__publish {
  display: flex;
  min-height: 3rem;
  flex: none;
  align-items: center;
  gap: var(--space-2);
  padding-inline: var(--space-5);
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.app-header__publish:hover {
  filter: brightness(1.08);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-inline-start: var(--space-2);
}

.app-header__session {
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
}

.app-header__session:hover {
  color: var(--color-primary);
}

@media (max-width: 48rem) {
  .app-header__inner {
    width: min(calc(100% - var(--space-6)), var(--content-wide));
    min-height: 3.625rem;
  }

  .app-header__navigation,
  .app-header__publish,
  .app-header__session {
    display: none;
  }

  .app-header__actions {
    margin-inline-start: auto;
  }
}
</style>
