<script setup lang="ts"></script>

<template>
  <div class="feed-divider" role="separator" aria-label="Você está em dia">
    <span>Você está em dia</span>
  </div>
</template>

<style scoped>
.feed-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.feed-divider::before,
.feed-divider::after {
  flex: 1;
  height: 1px;
  content: '';
  background: var(--color-border);
}
</style>
