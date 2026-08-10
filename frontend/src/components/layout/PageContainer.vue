<script setup lang="ts">
type ContainerSize = 'narrow' | 'feed' | 'wide'

withDefaults(
  defineProps<{
    size?: ContainerSize
  }>(),
  {
    size: 'wide',
  },
)
</script>

<template>
  <div class="page-container" :class="`page-container--${size}`">
    <slot />
  </div>
</template>

<style scoped>
.page-container {
  width: min(calc(100% - var(--space-8)), var(--content-wide));
  margin-inline: auto;
}

.page-container--narrow {
  max-width: var(--content-narrow);
}

.page-container--feed {
  max-width: var(--content-feed);
}

@media (max-width: 30rem) {
  .page-container {
    width: min(calc(100% - var(--space-6)), var(--content-wide));
  }
}
</style>
