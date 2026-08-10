<script setup lang="ts">
import BaseToast from '@/components/base/BaseToast.vue'
import { dismissNavigationError, navigationState } from '@/app/router'
</script>

<template>
  <a class="skip-link" href="#main-content">Pular para o conteúdo</a>
  <div
    v-if="navigationState.pending"
    class="navigation-progress"
    role="progressbar"
    aria-label="Carregando página"
  />
  <div v-if="navigationState.error" class="navigation-error">
    <BaseToast title="Erro de navegação" kind="error" @dismiss="dismissNavigationError">
      {{ navigationState.error }}
    </BaseToast>
  </div>
  <RouterView />
</template>

<style scoped>
.skip-link {
  position: fixed;
  z-index: 110;
  top: var(--space-2);
  left: var(--space-2);
  padding: var(--space-3) var(--space-4);
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  transform: translateY(calc(-100% - var(--space-4)));
  transition: transform var(--duration-fast) var(--ease-standard);
}

.skip-link:focus {
  transform: translateY(0);
}

.navigation-progress {
  position: fixed;
  z-index: 100;
  top: 0;
  left: 0;
  width: 40%;
  height: var(--space-1);
  background: var(--color-accent);
  transform-origin: left;
  animation: navigation-progress var(--duration-slow) var(--ease-emphasized) infinite alternate;
}

.navigation-error {
  position: fixed;
  z-index: 90;
  top: var(--space-4);
  right: var(--space-4);
  left: var(--space-4);
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
}

.navigation-error > * {
  pointer-events: auto;
}

@keyframes navigation-progress {
  from {
    transform: scaleX(0.2);
  }

  to {
    transform: translateX(150%) scaleX(1);
  }
}
</style>
