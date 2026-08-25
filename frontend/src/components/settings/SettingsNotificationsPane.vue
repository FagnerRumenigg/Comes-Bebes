<script setup lang="ts">
import { computed } from 'vue'

import BaseToggle from '@/components/base/BaseToggle.vue'
import {
  useNotificationPreferencesQuery,
  useUpdateNotificationPreferences,
  type NotificationPreferences,
} from '@/features/settings/notificationPreferences'
import { useAuthStore } from '@/stores/auth.store'

import SettingsRow from './SettingsRow.vue'
import SettingsSection from './SettingsSection.vue'

const authStore = useAuthStore()
const userId = computed(() => authStore.identity?.userId ?? '')
const enabled = computed(() => authStore.authenticated)

const preferencesQuery = useNotificationPreferencesQuery(userId, enabled)
const updateMutation = useUpdateNotificationPreferences(userId)

function preference(key: keyof NotificationPreferences): boolean {
  const fallback = key === 'notifyOnFollowedPublish' || key === 'notifyWeeklyEmail' ? false : true
  return preferencesQuery.data.value?.[key] ?? fallback
}

function update(key: keyof NotificationPreferences, value: boolean): void {
  updateMutation.mutate({ [key]: value })
}
</script>

<template>
  <div class="settings-notifications-pane">
    <h2>Avisos</h2>
    <p class="settings-notifications-pane__lead">Escolha sobre o que você quer ser avisado.</p>

    <template v-if="preferencesQuery.isPending.value">
      <p class="settings-notifications-pane__state">Carregando preferências...</p>
    </template>
    <template v-else-if="preferencesQuery.isError.value">
      <p class="settings-notifications-pane__state" role="alert">
        Não foi possível carregar suas preferências de aviso.
      </p>
    </template>
    <template v-else>
      <SettingsSection title="Sobre o que você publica">
        <SettingsRow title="Quando alguém guarda uma publicação sua">
          <BaseToggle
            hide-label
            label="Quando alguém guarda uma publicação sua"
            :model-value="preference('notifyOnSaved')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnSaved', $event)"
          />
        </SettingsRow>
        <SettingsRow title="Quando alguém reage a uma publicação sua">
          <BaseToggle
            hide-label
            label="Quando alguém reage a uma publicação sua"
            :model-value="preference('notifyOnReacted')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnReacted', $event)"
          />
        </SettingsRow>
        <SettingsRow title="Quando alguém faz a própria versão da sua receita">
          <BaseToggle
            hide-label
            label="Quando alguém faz a própria versão da sua receita"
            :model-value="preference('notifyOnMyVersion')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnMyVersion', $event)"
          />
        </SettingsRow>
      </SettingsSection>

      <SettingsSection title="Sobre o que você acompanha">
        <SettingsRow title="Quando entra coisa nova numa coleção que você segue">
          <BaseToggle
            hide-label
            label="Quando entra coisa nova numa coleção que você segue"
            :model-value="preference('notifyOnCollectionNewItem')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnCollectionNewItem', $event)"
          />
        </SettingsRow>
        <SettingsRow title="Quando alguém que você segue publica">
          <BaseToggle
            hide-label
            label="Quando alguém que você segue publica"
            :model-value="preference('notifyOnFollowedPublish')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnFollowedPublish', $event)"
          />
        </SettingsRow>
        <SettingsRow title="Quando alguém compartilha uma coleção com você">
          <BaseToggle
            hide-label
            label="Quando alguém compartilha uma coleção com você"
            :model-value="preference('notifyOnCollectionShared')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyOnCollectionShared', $event)"
          />
        </SettingsRow>
      </SettingsSection>

      <SettingsSection title="Por e-mail" note="Fora do aplicativo, mandamos só o essencial.">
        <SettingsRow title="Receber um resumo por e-mail" description="Uma vez por semana, no domingo">
          <BaseToggle
            hide-label
            label="Receber um resumo por e-mail"
            :model-value="preference('notifyWeeklyEmail')"
            :disabled="updateMutation.isPending.value"
            @update:model-value="update('notifyWeeklyEmail', $event)"
          />
        </SettingsRow>
      </SettingsSection>
    </template>
  </div>
</template>

<style scoped>
.settings-notifications-pane h2 {
  margin: 0 0 var(--space-2);
  font-family: var(--font-editorial);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-regular);
}

.settings-notifications-pane__lead {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.settings-notifications-pane__state {
  color: var(--color-text-secondary);
}
</style>
