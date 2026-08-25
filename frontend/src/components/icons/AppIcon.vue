<script setup lang="ts">
import { computed } from 'vue'

import { ICON_PATHS, type IconName } from './icon-paths'

const props = withDefaults(
  defineProps<{
    name: IconName
    size?: number
    strokeWidth?: number
  }>(),
  {
    size: 24,
    strokeWidth: 1.8,
  },
)

const markup = computed(() => ICON_PATHS[props.name])
</script>

<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    v-html="markup"
  />
</template>

<style scoped>
.icon {
  display: block;
  flex: none;
}
</style>
