<script setup lang="ts">
defineProps<{
  id?: string
  message?: string
}>()
</script>

<template>
  <p v-if="message || $slots.default" :id="id" class="base-field-error" role="alert">
    <slot>{{ message }}</slot>
  </p>
</template>

<style scoped>
.base-field-error {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-ui);
}
</style>
