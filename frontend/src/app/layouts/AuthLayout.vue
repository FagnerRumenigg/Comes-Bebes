<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import PageContainer from '@/components/layout/PageContainer.vue'
import BrandMark from '@/components/layout/BrandMark.vue'
import ThemeSwitch from '@/components/layout/ThemeSwitch.vue'

const route = useRoute()

// Texto do painel muda por tela (docs/telas/02) — cada uma fala com quem
// está nela: quem já conhece o produto vs. quem está chegando agora.
const manifesto = computed(() =>
  route.name === 'register'
    ? {
        eyebrow: 'Cozinha, memória e descoberta',
        statement: 'Descubra o sabor da autenticidade brasileira.',
      }
    : {
        eyebrow: 'Que bom ter você de volta',
        statement: 'A arte de saborear histórias.',
      },
)
</script>

<template>
  <div class="auth-layout">
    <aside class="auth-layout__editorial" aria-label="Apresentação do Comes&Bebes">
      <RouterLink class="auth-layout__brand" to="/">
        <BrandMark size="medium" />
        Comes&amp;Bebes
      </RouterLink>
      <div class="auth-layout__manifesto">
        <p class="auth-layout__eyebrow">{{ manifesto.eyebrow }}</p>
        <p class="auth-layout__statement">{{ manifesto.statement }}</p>
      </div>
    </aside>

    <main id="main-content" class="auth-layout__main">
      <div class="auth-layout__theme"><ThemeSwitch /></div>
      <div class="auth-layout__content">
        <PageContainer size="narrow">
          <RouterView />
          <nav class="auth-layout__legal" aria-label="Links institucionais">
            <RouterLink to="/privacidade">Privacidade</RouterLink>
            <RouterLink to="/termos">Termos</RouterLink>
            <RouterLink to="/faq">Ajuda</RouterLink>
          </nav>
          <small class="auth-layout__copyright">© 2026 Comes&amp;Bebes Publicações Ltda.</small>
        </PageContainer>
      </div>
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
  line-height: var(--line-height-tight);
}

.auth-layout__main {
  position: relative;
  display: flex;
  flex-direction: column;
  padding-block-start: var(--space-16);
  background: var(--color-background);
}

.auth-layout__content {
  display: grid;
  flex: 1;
  align-items: center;
  padding-block-end: var(--space-16);
}

.auth-layout__theme {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
}

.auth-layout__legal {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-5);
  margin-block-start: var(--space-8);
}

.auth-layout__legal a {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
}

.auth-layout__legal a:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.auth-layout__copyright {
  display: block;
  margin-block-start: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-align: center;
  opacity: 0.8;
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
    padding-block-start: var(--space-10);
  }

  .auth-layout__content {
    align-items: start;
    padding-block-end: var(--space-10);
  }
}
</style>
