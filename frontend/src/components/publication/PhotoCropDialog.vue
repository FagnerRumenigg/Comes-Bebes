<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Cropper, type CropperResult } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'

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
const objectUrl = ref('')
const isProcessing = ref(false)
let confirmed = false

watch(
  () => props.file,
  (file) => {
    if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = file ? URL.createObjectURL(file) : ''
    confirmed = false
  },
)

// O <dialog> nativo fica com display:none até showModal() ser chamado
// (BaseDialog faz isso depois de um nextTick). Se o Cropper montar antes
// disso, ele mede um container com 0x0 e nunca se recupera sozinho —
// então força um refresh assim que o diálogo realmente abre.
watch(
  () => props.open,
  async (isOpen) => {
    if (!isOpen) return
    await nextTick()
    // Mais um frame de folga: garante que o showModal() do BaseDialog (que
    // também espera um nextTick, em paralelo) já rodou e o layout foi
    // calculado antes de medir o container.
    requestAnimationFrame(() => cropperRef.value?.refresh())
  },
)

function handleOpenChange(value: boolean): void {
  emit('update:open', value)
}

function handleClose(): void {
  if (!confirmed) emit('cancel')
  confirmed = false
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
    <div class="photo-crop">
      <Cropper
        v-if="objectUrl"
        ref="cropperRef"
        class="photo-crop__cropper"
        :src="objectUrl"
        :stencil-props="{ aspectRatio: TARGET_ASPECT_RATIO }"
        image-restriction="fit-area"
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
