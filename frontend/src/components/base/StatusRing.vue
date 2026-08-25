<script setup lang="ts">
import { computed } from 'vue'

import AppIcon from '@/components/icons/AppIcon.vue'

const props = withDefaults(
  defineProps<{
    variant?: 'success' | 'warning'
  }>(),
  {
    variant: 'success',
  },
)

const icon = computed(() => (props.variant === 'success' ? 'check' : 'alert'))
</script>

<template>
  <span class="status-ring" :class="`status-ring--${variant}`">
    <AppIcon :name="icon" :size="36" :stroke-width="2.6" />
  </span>
</template>

<style scoped>
.status-ring {
  display: grid;
  width: 4.875rem;
  height: 4.875rem;
  margin-inline: auto;
  place-items: center;
  border-radius: var(--radius-pill);
}

.status-ring--success {
  color: var(--color-success);
  background: color-mix(in srgb, var(--color-success) 16%, var(--color-surface));
  border: 2px solid var(--color-success);
}

.status-ring--warning {
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 16%, var(--color-surface));
  border: 2px solid var(--color-danger);
}
</style>
