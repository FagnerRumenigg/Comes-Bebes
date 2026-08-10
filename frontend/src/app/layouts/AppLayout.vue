<script setup lang="ts">
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
    <MobileNavigation />
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-layout__main {
  padding-block: var(--space-10) var(--space-20);
}

@media (max-width: 48rem) {
  .app-layout__main {
    padding-block: var(--space-8) calc(var(--space-20) + env(safe-area-inset-bottom));
  }
}
</style>
