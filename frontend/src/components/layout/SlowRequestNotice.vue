<script setup lang="ts">
import { ref, watch } from 'vue'

import BaseDialog from '@/components/base/BaseDialog.vue'
import { backendStatus } from '@/composables/useBackendStatus'

const dismissed = ref(false)

// Cada novo ciclo de "requisição lenta" pode ser mostrado de novo, mesmo que
// o usuário tenha fechado o anterior.
watch(
  () => backendStatus.slowRequest,
  (slow) => {
    if (!slow) dismissed.value = false
  },
)
</script>

<template>
  <BaseDialog
    :open="backendStatus.slowRequest && !dismissed"
    title="Isso está demorando mais que o normal"
    description="Pode ser o processamento da imagem ou o servidor ligando depois de um tempo parado. Continue aguardando - já já volta ao normal."
    @update:open="dismissed = true"
  />
</template>
