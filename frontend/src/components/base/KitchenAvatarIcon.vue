<script setup lang="ts">
import { computed } from 'vue'

import { findKitchenAvatar } from '@/components/icons/kitchen-avatar-paths'

const props = defineProps<{ avatarKey: string }>()

const avatar = computed(() => findKitchenAvatar(props.avatarKey))
</script>

<template>
  <svg
    v-if="avatar"
    class="kitchen-avatar-icon"
    :class="`kitchen-avatar-icon--${avatar.pair}`"
    viewBox="0 0 32 32"
    aria-hidden="true"
    focusable="false"
  >
    <circle class="kitchen-avatar-icon__tile" cx="16" cy="16" r="16" />
    <g v-html="avatar.markup" />
  </svg>
</template>

<style scoped>
.kitchen-avatar-icon {
  display: block;
  width: 100%;
  height: 100%;
}

.kitchen-avatar-icon--a .kitchen-avatar-icon__tile {
  fill: var(--color-primary);
}

.kitchen-avatar-icon--a :deep(.ink) {
  fill: var(--color-accent);
}

.kitchen-avatar-icon--a :deep(.cut) {
  fill: var(--color-primary);
}

.kitchen-avatar-icon--b .kitchen-avatar-icon__tile {
  fill: var(--color-accent);
}

.kitchen-avatar-icon--b :deep(.ink) {
  fill: var(--color-primary);
}

.kitchen-avatar-icon--b :deep(.cut) {
  fill: var(--color-accent);
}

:deep(.ink) {
  stroke: var(--color-text);
  stroke-width: 1.8;
  stroke-linejoin: round;
  stroke-linecap: round;
}
</style>
