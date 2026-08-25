<script setup lang="ts"></script>

<template>
  <div class="sheet-page">
    <div class="sheet-page__sheet">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.sheet-page {
  display: flex;
  min-height: 100vh;
  justify-content: center;
  padding: var(--space-8) var(--space-4) var(--space-16);
  background: var(--color-surface);
}

.sheet-page__sheet {
  width: 100%;
  max-width: 35rem;
  height: fit-content;
  padding: var(--space-12) var(--space-11) var(--space-10);
  background: var(--color-background);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

@media (max-width: 40rem) {
  .sheet-page__sheet {
    padding: var(--space-8) var(--space-5) var(--space-7);
  }
}
</style>
