<script setup lang="ts">
import type { ThemePreference } from '@/stores/theme.store'
import RadioCardGroup from '@/components/base/RadioCardGroup.vue'
import BaseToggle from '@/components/base/BaseToggle.vue'
import { useTheme } from '@/composables/useTheme'
import { useReducedMotion } from '@/composables/useReducedMotion'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'

const THEME_OPTIONS = [
  { value: 'light', title: 'Claro', description: 'Fundo creme, letras escuras.' },
  { value: 'dark', title: 'Escuro', description: 'Fundo marrom, letras claras. Cansa menos à noite.' },
  {
    value: 'system',
    title: 'Igual ao aparelho',
    description: 'Acompanha o que você já escolheu no celular ou no computador.',
  },
]

const { preference, setTheme } = useTheme()
const { enabled: reduceMotion, setEnabled: setReduceMotion } = useReducedMotion()

function updateTheme(value: string): void {
  setTheme(value as ThemePreference)
}
</script>

<template>
  <div class="settings-appearance-pane">
    <h2>Aparência</h2>
    <p class="settings-appearance-pane__lead">Como o Comes&amp;Bebes se parece neste aparelho.</p>

    <SettingsSection>
      <RadioCardGroup :model-value="preference" :options="THEME_OPTIONS" @update:model-value="updateTheme" />
    </SettingsSection>

    <SettingsSection title="Movimento">
      <SettingsRow
        title="Reduzir animações"
        description="Deixa as telas mais paradas. Ajuda quem sente enjoo ou tontura."
      >
        <BaseToggle
          hide-label
          label="Reduzir animações"
          :model-value="reduceMotion"
          @update:model-value="setReduceMotion"
        />
      </SettingsRow>
    </SettingsSection>
  </div>
</template>

<style scoped>
.settings-appearance-pane h2 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-appearance-pane__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}
</style>
