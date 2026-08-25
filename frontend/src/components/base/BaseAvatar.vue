<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    size?: 'small' | 'medium' | 'large'
  }>(),
  {
    size: 'small',
  },
)

const initial = computed(() => props.name.trim().charAt(0).toUpperCase() || '?')
</script>

<template>
  <span class="base-avatar" :class="`base-avatar--${size}`" aria-hidden="true">{{ initial }}</span>
</template>

<style scoped>
.base-avatar {
  display: inline-grid;
  flex: none;
  place-items: center;
  color: var(--color-primary-contrast);
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  background: var(--color-secondary);
  border-radius: var(--radius-pill);
}

.base-avatar--small {
  width: 2rem;
  height: 2rem;
  font-size: var(--font-size-md);
}

.base-avatar--medium {
  width: 4rem;
  height: 4rem;
  font-size: var(--font-size-2xl);
}

.base-avatar--large {
  width: 6.5rem;
  height: 6.5rem;
  font-size: var(--font-size-4xl);
}
</style>
