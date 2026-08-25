<script setup lang="ts">
import DevicesPanel from '@/components/settings/DevicesPanel.vue'
</script>

<template>
  <section class="devices-view">
    <header class="devices-view__header">
      <p class="devices-view__eyebrow">Segurança</p>
      <h1>Meus dispositivos</h1>
      <p>Veja onde sua conta está conectada e revogue o acesso de quem você não reconhece.</p>
    </header>

    <DevicesPanel />
  </section>
</template>

<style scoped>
.devices-view {
  max-width: var(--content-feed);
  margin-inline: auto;
}
.devices-view__header {
  margin-block-end: var(--space-8);
}
.devices-view__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.devices-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.devices-view__header > p:last-child {
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
}
</style>
