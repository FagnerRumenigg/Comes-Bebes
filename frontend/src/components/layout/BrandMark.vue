<script setup lang="ts">
import brandNotebook from '@/assets/images/brand-recipe-notebook.png'

withDefaults(
  defineProps<{
    size?: 'small' | 'medium'
  }>(),
  {
    size: 'small',
  },
)
</script>

<template>
  <img
    class="brand-mark"
    :class="`brand-mark--${size}`"
    :src="brandNotebook"
    alt=""
    aria-hidden="true"
  />
</template>

<style scoped>
.brand-mark {
  display: block;
  flex: none;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.brand-mark--small {
  width: 2rem;
  height: 2rem;
}

.brand-mark--medium {
  width: 2.5rem;
  height: 2.5rem;
}
</style>
