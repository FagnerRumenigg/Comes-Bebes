<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
  }>(),
  {
    description: undefined,
  },
)
</script>

<template>
  <div class="settings-row">
    <div class="settings-row__info">
      <b>{{ title }}</b>
      <span v-if="description">{{ description }}</span>
    </div>
    <div v-if="$slots.default" class="settings-row__action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.settings-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-height: 4rem;
  padding-block: var(--space-3);
  border-block-end: 1px solid var(--color-border);
}

.settings-row:last-child {
  border-block-end: 0;
}

.settings-row__info {
  flex: 1;
  min-width: 0;
}

.settings-row__info b {
  display: block;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}

.settings-row__info span {
  display: block;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.settings-row__action {
  flex: none;
}
</style>
