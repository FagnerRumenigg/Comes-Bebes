<script setup lang="ts">
import { computed } from 'vue'

import type { PublicationResponseVisibility } from '@/api/generated/models'

const props = defineProps<{
  visibility: PublicationResponseVisibility
}>()

const label = computed(() => (props.visibility === 'INTERNAL' ? 'Somente comunidade' : 'Pública'))
</script>

<template>
  <span class="visibility-badge" :class="`visibility-badge--${visibility.toLowerCase()}`">
    {{ label }}
  </span>
</template>

<style scoped>
.visibility-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  line-height: var(--line-height-ui);
  background: color-mix(in srgb, var(--color-border) 42%, transparent);
  border-radius: var(--radius-pill);
}

.visibility-badge--internal {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
}
</style>
