<script setup lang="ts">
import BackendOfflineScreen from '@/components/layout/BackendOfflineScreen.vue'
import ErrorScreen from '@/components/layout/ErrorScreen.vue'
import SlowRequestNotice from '@/components/layout/SlowRequestNotice.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import PatchNotesModal from '@/components/patchnotes/PatchNotesModal.vue'
import { dismissNavigationError, navigationState } from '@/app/router'
import { appCrashed } from '@/composables/useAppCrash'
import { authNotice, dismissAuthNotice } from '@/composables/useAuthNotice'
import { backendStatus } from '@/composables/useBackendStatus'
import { useKeepAlivePing } from '@/composables/useKeepAlivePing'
import { onlineStatus } from '@/composables/useOnlineStatus'

useKeepAlivePing()

function reloadApp(): void {
  window.location.reload()
}

function goHomeAfterCrash(): void {
  window.location.href = '/'
}

function retryOffline(): void {
  onlineStatus.online = navigator.onLine
}
</script>

<template>
  <ErrorScreen v-if="appCrashed" cause="unknown" @retry="reloadApp" @home="goHomeAfterCrash" />
  <ErrorScreen v-else-if="!onlineStatus.online" cause="offline" @retry="retryOffline" />
  <BackendOfflineScreen v-else-if="backendStatus.offline" />
  <template v-else>
    <a class="skip-link" href="#main-content">Pular para o conteúdo</a>
    <div
      v-if="navigationState.pending"
      class="navigation-progress"
      role="progressbar"
      aria-label="Carregando página"
    />
    <div v-if="navigationState.error || authNotice.visible" class="global-toast">
      <BaseToast
        v-if="navigationState.error"
        title="Erro de navegação"
        kind="error"
        @dismiss="dismissNavigationError"
      >
        {{ navigationState.error }}
      </BaseToast>
      <BaseToast
        v-if="authNotice.visible"
        title="Entre para continuar"
        kind="warning"
        @dismiss="dismissAuthNotice"
      >
        Você precisa estar conectado para realizar essa ação.
      </BaseToast>
    </div>
    <RouterView />
    <PatchNotesModal />
    <SlowRequestNotice />
  </template>
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

.global-toast {
  position: fixed;
  z-index: 90;
  top: var(--space-4);
  right: var(--space-4);
  left: var(--space-4);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-3);
  justify-content: flex-end;
  pointer-events: none;
}

.global-toast > * {
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
