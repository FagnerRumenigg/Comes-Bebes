<script setup lang="ts">
import { computed } from 'vue'

import BaseIconButton from '@/components/base/BaseIconButton.vue'
import { useTheme } from '@/composables/useTheme'

const { resolvedTheme, setTheme } = useTheme()

const isDark = computed(() => resolvedTheme.value === 'dark')
const label = computed(() => (isDark.value ? 'Ativar tema claro' : 'Ativar tema escuro'))

function toggleTheme(): void {
  setTheme(isDark.value ? 'light' : 'dark')
}
</script>

<template>
  <BaseIconButton :label="label" :pressed="isDark" @click="toggleTheme">
    <svg class="theme-switch__icon" viewBox="0 0 24 24" aria-hidden="true">
      <path
        v-if="isDark"
        d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.36-6.36-1.42 1.42M7.06 16.94l-1.42 1.42m12.72 0-1.42-1.42M7.06 7.06 5.64 5.64M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z"
      />
      <path v-else d="M20.5 15.25A8.5 8.5 0 0 1 8.75 3.5a8.5 8.5 0 1 0 11.75 11.75Z" />
    </svg>
  </BaseIconButton>
</template>

<style scoped>
.theme-switch__icon {
  width: 1.25rem;
  height: 1.25rem;
  fill: none;
  stroke: currentcolor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.75;
}
</style>
