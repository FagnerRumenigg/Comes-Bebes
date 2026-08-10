<script setup lang="ts">
defineProps<{
  sourceId: string
  title?: string | null
}>()
</script>

<template>
  <p class="origin-reference">
    Inspirada em
    <RouterLink :to="`/publicacoes/${sourceId}`">{{
      title ?? 'uma receita da comunidade'
    }}</RouterLink>
  </p>
</template>

<style scoped>
.origin-reference {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.origin-reference a {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}
</style>
