<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    note?: string
  }>(),
  {
    title: undefined,
    note: undefined,
  },
)
</script>

<template>
  <div class="settings-section">
    <h3 v-if="title">{{ title }}</h3>
    <p v-if="note" class="settings-section__note">{{ note }}</p>
    <div class="settings-section__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.settings-section {
  margin-block-end: var(--space-8);
}

.settings-section h3 {
  margin-block-end: var(--space-1);
  font-family: var(--font-interface);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  letter-spacing: normal;
}

.settings-section__note {
  margin-block-end: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
