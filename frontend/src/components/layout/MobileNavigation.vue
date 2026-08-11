<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth.store'
import { showAuthNotice } from '@/composables/useAuthNotice'

const router = useRouter()
const authStore = useAuthStore()
const profileDestination = computed(() => `/u/${authStore.identity?.username ?? ''}`)

async function logout(): Promise<void> {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <nav class="mobile-navigation" aria-label="Navegação móvel">
    <RouterLink to="/">Início</RouterLink>
    <RouterLink to="/buscar">Buscar</RouterLink>
    <RouterLink v-if="authStore.authenticated" class="mobile-navigation__primary" to="/publicar">
      Publicar
    </RouterLink>
    <button v-else type="button" class="mobile-navigation__primary" @click="showAuthNotice">
      Publicar
    </button>
    <RouterLink v-if="authStore.authenticated" to="/salvos">Salvos</RouterLink>
    <button v-else type="button" @click="showAuthNotice">Salvos</button>
    <RouterLink v-if="authStore.authenticated" :to="profileDestination">Perfil</RouterLink>
    <RouterLink v-else to="/login">Entrar</RouterLink>
    <button
      v-if="authStore.authenticated"
      type="button"
      class="mobile-navigation__logout"
      aria-label="Sair da conta"
      @click="logout"
    >
      Sair
    </button>
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
  align-items: center;
  padding: var(--space-2) max(var(--space-2), env(safe-area-inset-right))
    max(var(--space-2), env(safe-area-inset-bottom)) max(var(--space-2), env(safe-area-inset-left));
  background: var(--color-surface-raised);
  border-block-start: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.mobile-navigation a,
.mobile-navigation button {
  display: grid;
  min-height: var(--control-min-size);
  place-items: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-align: center;
  text-decoration: none;
  background: transparent;
  border: 0;
}

.mobile-navigation a.router-link-exact-active {
  color: var(--color-primary);
}

.mobile-navigation__primary {
  color: var(--color-primary) !important;
  font-weight: var(--font-weight-bold) !important;
}

@media (max-width: 48rem) {
  .mobile-navigation {
    display: grid;
  }
}
</style>
