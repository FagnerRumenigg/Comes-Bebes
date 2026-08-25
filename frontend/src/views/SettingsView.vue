<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppIcon from '@/components/icons/AppIcon.vue'
import type { IconName } from '@/components/icons/icon-paths'
import SettingsAccessPane from '@/components/settings/SettingsAccessPane.vue'
import SettingsAccountPane from '@/components/settings/SettingsAccountPane.vue'
import SettingsAppearancePane from '@/components/settings/SettingsAppearancePane.vue'
import SettingsHelpPane from '@/components/settings/SettingsHelpPane.vue'
import SettingsNotificationsPane from '@/components/settings/SettingsNotificationsPane.vue'

type SectionId = 'conta' | 'acesso' | 'avisos' | 'aparencia' | 'ajuda'

const SECTIONS: Array<{ id: SectionId; label: string; icon: IconName }> = [
  { id: 'conta', label: 'Minha conta', icon: 'person' },
  { id: 'acesso', label: 'Entrar e aparelhos', icon: 'lock' },
  { id: 'avisos', label: 'Avisos', icon: 'bell' },
  { id: 'aparencia', label: 'Aparência', icon: 'sun' },
  { id: 'ajuda', label: 'Ajuda e sobre', icon: 'help' },
]

const route = useRoute()
const router = useRouter()

const activeSection = computed<SectionId>(() => {
  const value = route.params.secao
  const candidate = typeof value === 'string' ? value : ''
  return SECTIONS.some((section) => section.id === candidate) ? (candidate as SectionId) : 'conta'
})

function selectSection(id: SectionId): void {
  void router.replace({ name: 'settings', params: { secao: id } })
}
</script>

<template>
  <section class="settings-view">
    <div class="settings-view__cols">
      <aside class="settings-view__side">
        <h1>Configurações</h1>
        <nav class="settings-view__nav">
          <button
            v-for="section in SECTIONS"
            :key="section.id"
            type="button"
            :aria-current="activeSection === section.id ? 'page' : undefined"
            @click="selectSection(section.id)"
          >
            <AppIcon :name="section.icon" :size="21" :stroke-width="1.8" />
            {{ section.label }}
          </button>
        </nav>
      </aside>

      <main class="settings-view__pane">
        <SettingsAccountPane v-if="activeSection === 'conta'" />
        <SettingsAccessPane v-else-if="activeSection === 'acesso'" />
        <SettingsNotificationsPane v-else-if="activeSection === 'avisos'" />
        <SettingsAppearancePane v-else-if="activeSection === 'aparencia'" />
        <SettingsHelpPane v-else-if="activeSection === 'ajuda'" />
      </main>
    </div>
  </section>
</template>

<style scoped>
.settings-view {
  max-width: var(--content-wide, 72rem);
  margin-inline: auto;
}

.settings-view__cols {
  display: grid;
  grid-template-columns: 17rem 1fr;
  gap: var(--space-6);
}

.settings-view__side h1 {
  margin: 0 0 var(--space-4);
  font-family: var(--font-family-display);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-view__nav {
  display: grid;
  gap: 0.125rem;
}

.settings-view__nav button {
  display: flex;
  width: 100%;
  min-height: 3.25rem;
  align-items: center;
  gap: var(--space-3);
  padding-inline: var(--space-3);
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  text-align: left;
  background: none;
  border: 0;
  border-radius: var(--radius-md);
}

.settings-view__nav button:hover {
  background: var(--color-surface);
}

.settings-view__nav button[aria-current='page'] {
  color: var(--color-text);
  font-weight: var(--font-weight-bold);
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
}

.settings-view__pane {
  max-width: 40rem;
  padding-block: var(--space-2) var(--space-10);
}

@media (max-width: 56rem) {
  .settings-view__cols {
    grid-template-columns: 1fr;
  }

  .settings-view__nav {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    margin-block-end: var(--space-4);
    border-block-end: 1px solid var(--color-border);
    padding-block-end: var(--space-4);
  }

  .settings-view__nav button {
    width: auto;
    min-height: 2.75rem;
  }
}
</style>
