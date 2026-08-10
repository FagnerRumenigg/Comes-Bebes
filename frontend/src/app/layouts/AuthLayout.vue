<script setup lang="ts">
import PageContainer from '@/components/layout/PageContainer.vue'
import BrandMark from '@/components/layout/BrandMark.vue'
import ThemeSwitch from '@/components/layout/ThemeSwitch.vue'
</script>

<template>
  <div class="auth-layout">
    <aside class="auth-layout__editorial" aria-label="Apresentação do Comes&Bebes">
      <RouterLink class="auth-layout__brand" to="/">
        <BrandMark size="medium" />
        Comes&amp;Bebes
      </RouterLink>
      <div class="auth-layout__manifesto">
        <p class="auth-layout__eyebrow">Cozinha, memória e descoberta</p>
        <p class="auth-layout__statement">A arte de saborear histórias.</p>
      </div>
    </aside>

    <main id="main-content" class="auth-layout__main">
      <div class="auth-layout__theme"><ThemeSwitch /></div>
      <PageContainer size="narrow">
        <RouterView />
      </PageContainer>
    </main>
  </div>
</template>

<style scoped>
.auth-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(20rem, 1fr) minmax(28rem, 1fr);
}

.auth-layout__editorial {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: var(--space-12);
  color: var(--color-brand-text);
  background:
    linear-gradient(var(--color-overlay), var(--color-overlay)),
    url('@/assets/images/auth-editorial.png') center / cover no-repeat;
}

.auth-layout__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  color: inherit;
  font-family: var(--font-editorial);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  text-decoration: none;
}

.auth-layout__manifesto {
  max-width: 34rem;
}

.auth-layout__eyebrow {
  margin-block-end: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.auth-layout__statement {
  font-family: var(--font-editorial);
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-bold);
  line-height: var(--line-height-tight);
}

.auth-layout__main {
  position: relative;
  display: grid;
  align-items: center;
  padding-block: var(--space-16);
  background: var(--color-background);
}

.auth-layout__theme {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
}

@media (max-width: 56rem) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-layout__editorial {
    min-height: 12rem;
    padding: var(--space-6);
  }

  .auth-layout__manifesto {
    margin-block-start: var(--space-10);
  }

  .auth-layout__statement {
    font-size: var(--font-size-2xl);
  }

  .auth-layout__main {
    align-items: start;
    padding-block: var(--space-10);
  }
}
</style>
