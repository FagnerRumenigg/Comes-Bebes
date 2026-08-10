<script setup lang="ts">
import { useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import { useAuthStore } from '@/stores/auth.store'

import BrandMark from './BrandMark.vue'
import ThemeSwitch from './ThemeSwitch.vue'

const router = useRouter()
const authStore = useAuthStore()

async function logout(): Promise<void> {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <RouterLink class="app-header__brand" to="/" aria-label="Comes&Bebes — início">
        <BrandMark />
        <span>Comes&amp;Bebes</span>
      </RouterLink>

      <nav class="app-header__navigation" aria-label="Navegação principal">
        <RouterLink to="/buscar">Buscar</RouterLink>
        <RouterLink to="/publicar">Publicar</RouterLink>
        <RouterLink to="/salvos">Salvos</RouterLink>
        <RouterLink to="/notificacoes">Notificações</RouterLink>
      </nav>

      <div class="app-header__actions">
        <ThemeSwitch />
        <template v-if="authStore.authenticated && authStore.identity">
          <RouterLink class="app-header__session" :to="`/u/${authStore.identity.username}`">
            @{{ authStore.identity.username }}
          </RouterLink>
          <BaseButton class="app-header__logout" variant="ghost" @click="logout">Sair</BaseButton>
        </template>
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
  min-height: 4rem;
  align-items: center;
  gap: var(--space-8);
  margin-inline: auto;
}

.app-header__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text);
  font-family: var(--font-editorial);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  text-decoration: none;
}

.app-header__navigation {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-inline-start: auto;
}

.app-header__navigation a,
.app-header__session {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
}

.app-header__navigation a:hover,
.app-header__navigation a.router-link-active,
.app-header__session:hover {
  color: var(--color-primary);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.app-header__session {
  padding: var(--space-2) var(--space-3);
}

.app-header__logout {
  min-height: 2.25rem;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
}

@media (max-width: 48rem) {
  .app-header__inner {
    width: min(calc(100% - var(--space-6)), var(--content-wide));
  }

  .app-header__navigation,
  .app-header__session,
  .app-header__logout {
    display: none;
  }

  .app-header__actions {
    margin-inline-start: auto;
  }
}
</style>
