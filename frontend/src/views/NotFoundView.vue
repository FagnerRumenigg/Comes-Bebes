<template>
  <section class="not-found-view" aria-labelledby="not-found-title">
    <p class="not-found-view__eyebrow">Erro 404</p>
    <h1 id="not-found-title">Essa página não está na mesa.</h1>
    <p>O endereço pode ter mudado ou a publicação não existe mais.</p>
    <RouterLink class="not-found-view__link" to="/">Voltar para o feed</RouterLink>
  </section>
</template>

<style scoped>
.not-found-view {
  display: grid;
  max-width: var(--content-narrow);
  justify-items: start;
  gap: var(--space-4);
  margin-inline: auto;
  padding-block: var(--space-16);
}

.not-found-view__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.not-found-view h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: var(--font-size-3xl);
}

.not-found-view > p:not(.not-found-view__eyebrow) {
  margin: 0;
  color: var(--color-text-secondary);
}

.not-found-view__link {
  display: inline-flex;
  min-height: var(--control-min-size);
  align-items: center;
  padding-inline: var(--space-5);
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

.not-found-view__link:hover {
  filter: brightness(1.08);
}
</style>
