<script setup lang="ts">
const commitHash = __APP_COMMIT_HASH__
</script>

<template>
  <footer class="app-footer">
    <span class="app-footer__name">Comes&amp;Bebes</span>
    <span>Feito por -A</span>
    <span class="app-footer__version">v{{ commitHash }}</span>
  </footer>
</template>

<style scoped>
.app-footer {
  display: grid;
  gap: var(--space-2);
  justify-items: center;
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-align: center;
  border-block-start: 1px solid var(--color-border);
}

.app-footer__name {
  font-weight: var(--font-weight-semibold);
}

.app-footer__version {
  font-family: var(--font-family-mono, monospace);
}
</style>
