<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth.store'

const authStore = useAuthStore()
const profileDestination = computed(() =>
  authStore.identity ? `/u/${authStore.identity.username}` : '/login',
)
</script>

<template>
  <nav class="mobile-navigation" aria-label="Navegação móvel">
    <RouterLink to="/">Início</RouterLink>
    <RouterLink to="/buscar">Buscar</RouterLink>
    <RouterLink class="mobile-navigation__primary" to="/publicar">Publicar</RouterLink>
    <RouterLink to="/salvos">Salvos</RouterLink>
    <RouterLink :to="profileDestination">Perfil</RouterLink>
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
  grid-template-columns: repeat(5, 1fr);
  align-items: center;
  padding: var(--space-2) max(var(--space-2), env(safe-area-inset-right))
    max(var(--space-2), env(safe-area-inset-bottom)) max(var(--space-2), env(safe-area-inset-left));
  background: var(--color-surface-raised);
  border-block-start: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.mobile-navigation a {
  display: grid;
  min-height: var(--control-min-size);
  place-items: center;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-align: center;
  text-decoration: none;
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
