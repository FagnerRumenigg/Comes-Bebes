<script setup lang="ts">
import { useRouter } from 'vue-router'

import BaseAvatar from '@/components/base/BaseAvatar.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import { useAccountInfo } from '@/composables/useAccountInfo'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth.store'

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()
const authStore = useAuthStore()
const { resolvedTheme, setTheme } = useTheme()
const { displayName } = useAccountInfo()

async function goToProfile(): Promise<void> {
  emit('close')
  await router.push(`/u/${authStore.identity?.username ?? ''}`)
}

async function goToSettings(): Promise<void> {
  emit('close')
  await router.push('/configuracoes')
}

async function goToHelp(): Promise<void> {
  emit('close')
  await router.push('/configuracoes/ajuda')
}

async function logout(): Promise<void> {
  emit('close')
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <div class="account-menu-content">
    <div class="account-menu-content__head">
      <BaseAvatar :name="displayName ?? authStore.identity?.username ?? '?'" size="small" />
      <div>
        <b>{{ displayName ?? `@${authStore.identity?.username}` }}</b>
        <span v-if="displayName" class="account-menu-content__username"
          >@{{ authStore.identity?.username }}</span
        >
      </div>
    </div>

    <button class="account-menu-content__item" type="button" @click="goToProfile">
      <AppIcon name="person" :size="20" :stroke-width="1.8" />
      Meu perfil
    </button>

    <button class="account-menu-content__item" type="button" @click="goToSettings">
      <AppIcon name="settings" :size="20" :stroke-width="1.8" />
      Configurações
    </button>

    <button class="account-menu-content__item" type="button" @click="goToHelp">
      <AppIcon name="help" :size="20" :stroke-width="1.8" />
      Ajuda
    </button>

    <div class="account-menu-content__theme">
      <span class="account-menu-content__theme-label">Aparência</span>
      <div class="account-menu-content__seg" role="group" aria-label="Aparência">
        <button
          type="button"
          :aria-pressed="resolvedTheme === 'light'"
          @click="setTheme('light')"
        >
          <AppIcon name="sun" :size="17" :stroke-width="1.9" />
          Claro
        </button>
        <button type="button" :aria-pressed="resolvedTheme === 'dark'" @click="setTheme('dark')">
          <AppIcon name="moon" :size="17" :stroke-width="1.9" />
          Escuro
        </button>
      </div>
    </div>

    <hr />

    <button
      class="account-menu-content__item account-menu-content__item--danger"
      type="button"
      @click="logout"
    >
      <AppIcon name="logout" :size="20" :stroke-width="1.8" />
      Sair da conta
    </button>
  </div>
</template>

<style scoped>
.account-menu-content {
  display: grid;
  gap: var(--space-1);
}

.account-menu-content__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-2) var(--space-3);
  margin-bottom: var(--space-1);
  border-block-end: 1px solid var(--color-border);
}

.account-menu-content__head b {
  display: block;
  font-size: var(--font-size-md);
}

.account-menu-content__username {
  display: block;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.account-menu-content__item {
  display: flex;
  width: 100%;
  min-height: var(--control-min-size);
  align-items: center;
  gap: var(--space-3);
  padding-inline: var(--space-2);
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-md);
  text-align: left;
  background: none;
  border: 0;
  border-radius: var(--radius-md);
}

.account-menu-content__item:hover {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
}

.account-menu-content__item--danger {
  color: var(--color-danger);
}

.account-menu-content__item--danger:hover {
  background: color-mix(in srgb, var(--color-danger) 12%, var(--color-surface));
}

.account-menu-content__theme {
  padding: var(--space-2);
}

.account-menu-content__theme-label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.account-menu-content__seg {
  display: flex;
  gap: var(--space-1);
  padding: var(--space-1);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
}

.account-menu-content__seg button {
  display: flex;
  flex: 1;
  min-height: 2.5rem;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: none;
  border: 0;
  border-radius: var(--radius-sm);
}

.account-menu-content__seg button[aria-pressed='true'] {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
}

hr {
  margin-block: var(--space-2);
  border: 0;
  border-block-start: 1px solid var(--color-border);
}
</style>
