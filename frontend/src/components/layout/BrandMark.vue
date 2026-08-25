<script setup lang="ts">
withDefaults(
  defineProps<{
    size?: 'tiny' | 'small' | 'medium'
  }>(),
  {
    size: 'small',
  },
)
</script>

<template>
  <svg
    class="brand-mark"
    :class="`brand-mark--${size}`"
    viewBox="0 0 32 32"
    aria-hidden="true"
    focusable="false"
  >
    <rect class="brand-mark__tile" width="32" height="32" rx="8" />
    <path
      class="brand-mark__book"
      d="M10 7.5h12a1.6 1.6 0 0 1 1.6 1.6v13.8a1.6 1.6 0 0 1-1.6 1.6H10z"
    />
    <rect class="brand-mark__spine" x="8.4" y="7.5" width="2.2" height="17" rx="1.1" />
    <path class="brand-mark__ribbon" d="M16.6 7.5h4.2v6.6l-2.1-1.7-2.1 1.7z" />
  </svg>
</template>

<style scoped>
.brand-mark {
  display: block;
  flex: none;
  border-radius: 26%;
}

.brand-mark--tiny {
  width: 1.25rem;
  height: 1.25rem;
}

.brand-mark--small {
  width: 2rem;
  height: 2rem;
}

.brand-mark--medium {
  width: 2.5rem;
  height: 2.5rem;
}

.brand-mark__tile {
  fill: var(--color-primary);
}

.brand-mark__book {
  fill: var(--color-accent);
}

.brand-mark__spine {
  fill: var(--color-accent);
  opacity: 0.45;
}

.brand-mark__ribbon {
  fill: var(--color-primary);
}
</style>
