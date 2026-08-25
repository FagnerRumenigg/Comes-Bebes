<script setup lang="ts">
import { computed } from 'vue'

import type { PublicationResponseVisibility } from '@/api/generated/models'

const props = defineProps<{
  visibility: PublicationResponseVisibility
}>()

// Vocabulário exato de produto5.md v5 §4/§6.4 — "Interno" está banido da interface.
const LABELS: Record<PublicationResponseVisibility, string> = {
  PUBLIC: 'Público',
  INTERNAL: 'Só para quem tem conta',
  PRIVATE: 'Só para mim',
}
const label = computed(() => LABELS[props.visibility])
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

.visibility-badge--private {
  /* Vermelho é exclusivo de erro real (produto5.md v5 §3.4) — "Só para mim" não é
     um alerta, então usa o mesmo tom neutro do rótulo padrão, só com borda para
     se diferenciar visualmente de "Público". */
  border: 1px solid var(--color-border);
}
</style>
