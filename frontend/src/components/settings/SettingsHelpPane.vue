<script setup lang="ts">
import { useRouter } from 'vue-router'

import BaseButton from '@/components/base/BaseButton.vue'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'

const commitHash = __APP_COMMIT_HASH__
const router = useRouter()

function showWelcomeAgain(): void {
  void router.push({ name: 'welcome' })
}
</script>

<template>
  <div class="settings-help-pane">
    <h2>Ajuda e sobre</h2>
    <p class="settings-help-pane__lead">Dúvidas, regras e informações do aplicativo.</p>

    <SettingsSection>
      <SettingsRow title="Como usar o Comes&Bebes" description="Perguntas comuns, explicadas com calma">
        <BaseButton variant="secondary" @click="router.push({ name: 'faq' })">Abrir</BaseButton>
      </SettingsRow>
      <SettingsRow title="Falar com a gente" description="Achou um problema ou tem uma ideia?">
        <BaseButton variant="secondary" @click="router.push({ name: 'feedback' })">Escrever</BaseButton>
      </SettingsRow>
      <SettingsRow title="Ver as boas-vindas de novo" description="Aquela tela do primeiro acesso">
        <BaseButton variant="secondary" @click="showWelcomeAgain">Ver</BaseButton>
      </SettingsRow>
    </SettingsSection>

    <SettingsSection title="Documentos">
      <SettingsRow title="Termos de Serviço">
        <BaseButton variant="secondary" @click="router.push({ name: 'terms' })">Ler</BaseButton>
      </SettingsRow>
      <SettingsRow title="Política de Privacidade">
        <BaseButton variant="secondary" @click="router.push({ name: 'privacy' })">Ler</BaseButton>
      </SettingsRow>
    </SettingsSection>

    <p class="settings-help-pane__version">Comes&amp;Bebes · versão {{ commitHash }}</p>
  </div>
</template>

<style scoped>
.settings-help-pane h2 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-help-pane__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.settings-help-pane__version {
  margin: var(--space-4) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-align: center;
}
</style>
