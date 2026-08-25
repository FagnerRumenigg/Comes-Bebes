<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

import BaseAvatar from '@/components/base/BaseAvatar.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { useAccountInfo } from '@/composables/useAccountInfo'
import { useAuthStore } from '@/stores/auth.store'

import AccountMenuContent from './AccountMenuContent.vue'

const { displayName } = useAccountInfo()

// Mesmo ponto de corte de MobileNavigation.vue/AppHeader.vue (48rem) — decide
// se abre o dropdown (web) ou a folha (mobile). Renderizar só uma variante por
// vez evita ter dois "Sair da conta" no DOM ao mesmo tempo (um só escondido
// por CSS), o que confunde leitor de tela e automação.
const MOBILE_BREAKPOINT_QUERY = '(max-width: 48rem)'
const isMobile = ref(false)
let mediaQuery: MediaQueryList | undefined

const authStore = useAuthStore()
const open = ref(false)
const root = ref<HTMLElement>()

function toggle(): void {
  open.value = !open.value
}

function close(): void {
  open.value = false
}

function handleDocumentClick(event: MouseEvent): void {
  if (root.value && !root.value.contains(event.target as Node)) close()
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') close()
}

watch(open, (isOpen) => {
  // No mobile o fechamento é só pelo backdrop/Escape — o clique dentro da
  // folha não conta como "fora" porque ela é teleportada para <body>, fora
  // de `root`.
  if (isOpen && !isMobile.value) {
    document.addEventListener('click', handleDocumentClick)
  } else {
    document.removeEventListener('click', handleDocumentClick)
  }
  if (isOpen) {
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

function handleMediaChange(event: MediaQueryListEvent): void {
  isMobile.value = event.matches
}

onMounted(() => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
  mediaQuery = window.matchMedia(MOBILE_BREAKPOINT_QUERY)
  isMobile.value = mediaQuery.matches
  mediaQuery.addEventListener('change', handleMediaChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
  mediaQuery?.removeEventListener('change', handleMediaChange)
})
</script>

<template>
  <div ref="root" class="account-menu">
    <!-- Web: botão com avatar + nome, abre um menu suspenso -->
    <button
      class="account-menu__trigger account-menu__trigger--web"
      type="button"
      aria-haspopup="true"
      :aria-expanded="open"
      @click="toggle"
    >
      <BaseAvatar :name="displayName ?? authStore.identity?.username ?? '?'" size="small" />
      <span>{{ displayName ?? `@${authStore.identity?.username}` }}</span>
      <AppIcon name="chevron-down" :size="16" :stroke-width="2.2" class="account-menu__chevron" />
    </button>

    <!-- Mobile: só o avatar, abre uma folha na base da tela -->
    <button
      class="account-menu__trigger account-menu__trigger--mobile"
      type="button"
      aria-label="Sua conta"
      :aria-expanded="open"
      @click="toggle"
    >
      <BaseAvatar :name="displayName ?? authStore.identity?.username ?? '?'" size="small" />
    </button>

    <div v-if="open && !isMobile" class="account-menu__panel">
      <AccountMenuContent @close="close" />
    </div>

    <Teleport v-if="open && isMobile" to="body">
      <div class="account-menu__sheet-backdrop" @click="close" />
      <div class="account-menu__sheet" role="dialog" aria-label="Sua conta">
        <span class="account-menu__grab" aria-hidden="true" />
        <AccountMenuContent @close="close" />
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.account-menu {
  position: relative;
  flex: none;
}

.account-menu__trigger {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.account-menu__trigger--web {
  min-height: 2.75rem;
  padding: 0.25rem 0.75rem 0.25rem 0.25rem;
}

.account-menu__trigger--web:hover {
  background: var(--color-surface);
}

.account-menu__chevron {
  color: var(--color-text-secondary);
}

.account-menu__trigger--mobile {
  display: none;
  width: 2.75rem;
  height: 2.75rem;
  padding: 0;
  border-radius: var(--radius-pill);
}

.account-menu__panel {
  position: absolute;
  z-index: 30;
  top: calc(100% + var(--space-2));
  right: 0;
  width: 17rem;
  padding: var(--space-2);
  background: var(--color-background);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.account-menu__sheet-backdrop {
  position: fixed;
  z-index: 150;
  inset: 0;
  background: var(--color-overlay);
}

.account-menu__sheet {
  position: fixed;
  z-index: 160;
  right: 0;
  bottom: 0;
  left: 0;
  max-width: 28rem;
  padding: var(--space-3) var(--space-4) calc(var(--space-6) + env(safe-area-inset-bottom));
  margin-inline: auto;
  background: var(--color-background);
  border-start-start-radius: var(--radius-xl);
  border-start-end-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.account-menu__grab {
  display: block;
  width: 2.75rem;
  height: 0.25rem;
  margin: var(--space-1) auto var(--space-4);
  background: var(--color-border);
  border-radius: var(--radius-pill);
}

@media (max-width: 48rem) {
  .account-menu__trigger--web {
    display: none;
  }

  .account-menu__trigger--mobile {
    display: grid;
    place-items: center;
  }

  .account-menu__panel {
    display: none;
  }
}

@media (min-width: 48.01rem) {
  .account-menu__sheet-backdrop,
  .account-menu__sheet {
    display: none;
  }
}
</style>
