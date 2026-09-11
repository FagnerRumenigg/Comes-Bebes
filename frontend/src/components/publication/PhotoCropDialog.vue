<script setup lang="ts">
import { ref, watch } from 'vue'
import { Cropper, type CropperResult } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'

import { apiRequest } from '@/api/client'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'

// A proporção precisa bater com a do card no feed (PublicationImage.vue,
// aspect-ratio: 4/5) — senão o enquadramento escolhido aqui não é o que
// as pessoas veem depois.
const TARGET_ASPECT_RATIO = 4 / 5

const props = defineProps<{
  open: boolean
  file: File | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: [file: File]
  cancel: []
}>()

const cropperRef = ref<InstanceType<typeof Cropper>>()
const cropContainerRef = ref<HTMLElement>()
const objectUrl = ref('')
const isProcessing = ref(false)
let confirmed = false
const diagnosticsEnabled = import.meta.env.VITE_ENABLE_PHOTO_CROP_DIAGNOSTICS === 'true'
const diagnosticSessionId = crypto.randomUUID()
const openedAt = ref(0)

function sendDiagnostic(event: string, extra: Record<string, unknown> = {}): void {
  if (!diagnosticsEnabled || !props.file) return
  void apiRequest({
    url: '/diagnostics/photo-crop',
    method: 'POST',
    data: {
      sessionId: diagnosticSessionId,
      userAgent: navigator.userAgent,
      screenWidth: window.screen.width,
      screenHeight: window.screen.height,
      viewportWidth: window.innerWidth,
      viewportHeight: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio,
      fileType: props.file.type,
      fileName: props.file.name,
      fileSize: props.file.size,
      elapsedMs: openedAt.value ? performance.now() - openedAt.value : null,
      event,
      ...extra,
    },
  }).catch(() => undefined)
}

watch(
  () => props.file,
  (file) => {
    if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = file ? URL.createObjectURL(file) : ''
    confirmed = false
  },
)

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) openedAt.value = performance.now()
  },
)

function handleOpenChange(value: boolean): void {
  emit('update:open', value)
}

function handleClose(): void {
  if (!confirmed) emit('cancel')
  confirmed = false
}

function handleCropperReady(): void {
  const element = cropContainerRef.value
  sendDiagnostic('ready', {
    cropperReady: true,
    cropperWidth: element?.clientWidth ?? null,
    cropperHeight: element?.clientHeight ?? null,
  })
}

function handleCropperError(): void {
  sendDiagnostic('error', { cropperReady: false, error: 'cropper-error' })
}

function requestCancel(): void {
  emit('update:open', false)
}

function buildFileName(originalName: string | undefined): string {
  const base = (originalName ?? 'foto').replace(/\.[^./\\]+$/, '')
  return `${base}.jpg`
}

function confirm(): void {
  const result = cropperRef.value?.getResult() as CropperResult | undefined
  const canvas = result?.canvas
  if (!canvas) return
  isProcessing.value = true
  canvas.toBlob(
    (blob) => {
      isProcessing.value = false
      if (!blob) return
      confirmed = true
      const croppedFile = new File([blob], buildFileName(props.file?.name), {
        type: 'image/jpeg',
      })
      emit('update:open', false)
      emit('confirm', croppedFile)
    },
    'image/jpeg',
    0.9,
  )
}
</script>

<template>
  <BaseDialog
    :open="open"
    title="Ajuste o enquadramento"
    description="Arraste pra reposicionar e use o zoom pra escolher qual parte da foto aparece."
    @update:open="handleOpenChange"
    @close="handleClose"
  >
    <div ref="cropContainerRef" class="photo-crop">
      <!-- Sem limite de canvas, fotos de câmera de celular (bem maiores que
           telas comuns, às vezes 4000px+ de lado) estouram o tamanho máximo
           de canvas de alguns navegadores/GPUs mobile - a imagem some (fica
           em branco) em vez de aparecer. 4096 é um teto seguro na prática
           pra esse fim (recorte 4:5 pra foto de publicação não precisa de
           mais que isso). -->
      <Cropper
        v-if="objectUrl"
        ref="cropperRef"
        class="photo-crop__cropper"
        :src="objectUrl"
        :stencil-props="{ aspectRatio: TARGET_ASPECT_RATIO }"
        image-restriction="fit-area"
        :canvas="{ maxWidth: 4096, maxHeight: 4096 }"
        @ready="handleCropperReady"
        @error="handleCropperError"
      />
    </div>

    <template #actions>
      <BaseButton variant="secondary" type="button" :disabled="isProcessing" @click="requestCancel">
        Cancelar
      </BaseButton>
      <BaseButton type="button" :loading="isProcessing" @click="confirm">Usar essa foto</BaseButton>
    </template>
  </BaseDialog>
</template>

<style scoped>
.photo-crop {
  display: grid;
  /* O Cropper precisa de uma altura concreta pra se dimensionar — com só
     max-height (ou herdando altura automática dos wrappers), ele não tem
     como calcular a área de arraste e acaba mostrando a imagem inteira,
     sem permitir mover/dar zoom. */
  height: min(60vh, 32rem);
}

.photo-crop__cropper {
  width: 100%;
  height: 100%;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}
</style>
