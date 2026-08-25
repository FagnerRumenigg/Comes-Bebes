<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <div class="settings-zone">
    <h3>{{ title }}</h3>
    <slot />
  </div>
</template>

<style scoped>
.settings-zone {
  padding: var(--space-5) var(--space-6);
  margin-block-start: var(--space-8);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.settings-zone h3 {
  margin-block-end: var(--space-3);
  font-family: var(--font-interface);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  letter-spacing: normal;
}

.settings-zone :deep(.settings-row) {
  border-block-end-color: var(--color-border);
}
</style>
