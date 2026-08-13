<script setup lang="ts">
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import MobileNavigation from '@/components/layout/MobileNavigation.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
</script>

<template>
  <div class="app-layout">
    <AppHeader />
    <main id="main-content" class="app-layout__main">
      <PageContainer>
        <RouterView />
      </PageContainer>
    </main>
    <div class="app-layout__footer">
      <AppFooter />
    </div>
    <MobileNavigation />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-layout__main {
  flex: 1;
  padding-block: var(--space-10) var(--space-20);
}

@media (max-width: 48rem) {
  .app-layout__main {
    padding-block: var(--space-8) calc(var(--space-20) + env(safe-area-inset-bottom));
  }

  /* 4rem casa com o min-height de .mobile-navigation (MobileNavigation.vue) */
  .app-layout__footer {
    padding-block-end: calc(4rem + env(safe-area-inset-bottom));
  }
}
</style>
