<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import ThemeSwitch from '@/components/layout/ThemeSwitch.vue'
import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const authStore = useAuthStore()
const isLoggingOut = ref(false)

async function logout(): Promise<void> {
  if (isLoggingOut.value) return

  isLoggingOut.value = true
  try {
    await authStore.logout()
    await router.push('/login')
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<template>
  <div class="admin-layout">
    <header class="admin-layout__header">
      <PageContainer>
        <div class="admin-layout__header-inner">
          <RouterLink class="admin-layout__brand" to="/admin/moderacao">
            Comes&amp;Bebes <span>Administração</span>
          </RouterLink>
          <nav aria-label="Navegação administrativa">
            <RouterLink to="/admin/moderacao">Moderação</RouterLink>
            <RouterLink to="/">Voltar ao site</RouterLink>
          </nav>
          <div class="admin-layout__actions">
            <ThemeSwitch />
            <BaseButton variant="ghost" :loading="isLoggingOut" @click="logout">Sair</BaseButton>
          </div>
        </div>
      </PageContainer>
    </header>

    <main id="main-content" class="admin-layout__main">
      <PageContainer>
        <RouterView />
      </PageContainer>
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-layout__header {
  background: var(--color-surface-raised);
  border-block-end: 1px solid var(--color-border);
}

.admin-layout__header-inner {
  display: flex;
  min-height: 4.5rem;
  align-items: center;
  gap: var(--space-6);
}

.admin-layout__brand {
  display: grid;
  color: var(--color-text);
  font-family: var(--font-editorial);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  line-height: var(--line-height-tight);
  text-decoration: none;
}

.admin-layout__brand span {
  color: var(--color-text-secondary);
  font-family: var(--font-interface);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.admin-layout nav {
  display: flex;
  gap: var(--space-5);
  margin-inline-start: auto;
}

.admin-layout nav a {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.admin-layout nav a:hover,
.admin-layout nav a.router-link-active {
  color: var(--color-primary);
}

.admin-layout__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.admin-layout__actions :deep(button) {
  min-height: 2.25rem;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
}

.admin-layout__main {
  padding-block: var(--space-10) var(--space-20);
}

@media (max-width: 40rem) {
  .admin-layout__brand span,
  .admin-layout nav a:last-child {
    display: none;
  }

  .admin-layout__header-inner {
    gap: var(--space-3);
  }
}
</style>
