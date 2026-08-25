<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import {
  useCreateMyVersion,
  useCreateUpload,
  useGetPublicationById,
  useGetPublicationRecipe,
  useReportPhotoValidationFeedback,
} from '@/api/generated/publications/publications'
import type { CreateRecipeRequest } from '@/api/generated/models'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import RadioCardGroup from '@/components/base/RadioCardGroup.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import StatusRing from '@/components/base/StatusRing.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import IngredientEditor, {
  type IngredientDraft,
} from '@/components/publication/IngredientEditor.vue'
import PreparationStepsEditor from '@/components/publication/PreparationStepsEditor.vue'
import TagEditor from '@/components/publication/TagEditor.vue'
import {
  deleteDraft,
  getDraft,
  hasDraftContent,
  listDrafts,
  saveDraft,
  type PublicationDraft,
} from '@/features/publications/drafts'
import { PUBLICATION_VISIBILITY_OPTIONS } from '@/features/publications/visibilityOptions'
import { useAccountInfo } from '@/composables/useAccountInfo'
import { resolveImageUrl } from '@/utils/resolveImageUrl'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const sourceId = ref((route.params.sourceId as string | undefined) ?? '')
const routeDraftId = computed(() => (route.params.draftId as string | undefined) ?? '')
const isMyVersion = computed(() => Boolean(sourceId.value))
const sourceQuery = useGetPublicationById(sourceId, {
  query: { enabled: computed(() => isMyVersion.value) },
})
const recipeQuery = useGetPublicationRecipe(sourceId, {
  query: { enabled: computed(() => isMyVersion.value) },
})

const type = ref<'DISH' | 'RECIPE'>('DISH')
const visibility = ref<'PUBLIC' | 'INTERNAL' | 'PRIVATE'>('PUBLIC')
const VISIBILITY_OPTIONS = PUBLICATION_VISIBILITY_OPTIONS

// Pré-seleciona com a preferência salva em Configurações → Minha conta
// (docs/telas/09-configuracoes.html), sem sobrescrever se a pessoa já mexeu
// no campo ou está retomando um rascunho.
const { defaultPublicationVisibility } = useAccountInfo()
const visibilityTouched = ref(false)
watch(defaultPublicationVisibility, (value) => {
  if (value && !visibilityTouched.value) visibility.value = value
})
watch(visibility, () => {
  visibilityTouched.value = true
})
const title = ref('')
const description = ref('')
const titleSuffix = ref('')
const changeSummary = ref('')
const instructions = ref('')
const yieldQuantity = ref('')
const yieldUnit = ref('')
const image = ref<File | null>(null)
const imagePreview = ref('')
const imageError = ref('')
const formError = ref('')
const ingredientError = ref('')
const fieldErrors = reactive<Record<string, string>>({})
const published = ref(false)
const publishedId = ref('')
const ingredients = ref<IngredientDraft[]>([{ name: '', quantity: '', unit: '', note: '' }])
const tags = ref<string[]>([])

const rateLimitedUntil = ref<number | null>(null)
const cooldownNow = ref(Date.now())
let cooldownTicker: ReturnType<typeof setInterval> | undefined
const cooldownSecondsRemaining = computed(() =>
  rateLimitedUntil.value === null
    ? 0
    : Math.max(0, Math.ceil((rateLimitedUntil.value - cooldownNow.value) / 1000)),
)
const isRateLimited = computed(() => cooldownSecondsRemaining.value > 0)

function startRateLimitCooldown(nextAvailableAt: string): void {
  const target = new Date(nextAvailableAt).getTime()
  if (Number.isNaN(target)) return
  rateLimitedUntil.value = target
  cooldownNow.value = Date.now()
  if (cooldownTicker) clearInterval(cooldownTicker)
  cooldownTicker = setInterval(() => {
    cooldownNow.value = Date.now()
    if (cooldownNow.value >= target) {
      clearInterval(cooldownTicker)
      cooldownTicker = undefined
      rateLimitedUntil.value = null
    }
  }, 1_000)
}

const draftId = ref<string>(crypto.randomUUID())
let draftCreatedAt = new Date().toISOString()
const lastDraftSavedAt = ref<string | null>(null)
const existingDraftsCount = ref(0)
let autosaveTimer: ReturnType<typeof setInterval> | undefined

const savedAtFormatter = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })
function formatSavedTime(iso: string): string {
  return savedAtFormatter.format(new Date(iso))
}

function currentDraftSnapshot(): PublicationDraft {
  return {
    id: draftId.value,
    mode: isMyVersion.value ? 'MY_VERSION' : 'CREATE',
    sourceId: isMyVersion.value ? sourceId.value : null,
    createdAt: draftCreatedAt,
    updatedAt: new Date().toISOString(),
    type: type.value,
    visibility: visibility.value,
    title: title.value,
    description: description.value,
    titleSuffix: titleSuffix.value,
    changeSummary: changeSummary.value,
    instructions: instructions.value,
    yieldQuantity: yieldQuantity.value,
    yieldUnit: yieldUnit.value,
    ingredients: ingredients.value.map((item) => ({ ...item })),
    image: image.value,
    tags: [...tags.value],
  }
}

async function autosaveDraft(): Promise<void> {
  if (published.value) return
  const snapshot = currentDraftSnapshot()
  if (!hasDraftContent(snapshot)) return
  await saveDraft(snapshot)
  lastDraftSavedAt.value = snapshot.updatedAt
}

async function loadDraft(id: string): Promise<void> {
  const draft = await getDraft(id)
  if (!draft) return
  draftId.value = draft.id
  draftCreatedAt = draft.createdAt
  sourceId.value = draft.sourceId ?? ''
  type.value = draft.type
  visibility.value = draft.visibility
  title.value = draft.title
  description.value = draft.description
  titleSuffix.value = draft.titleSuffix
  changeSummary.value = draft.changeSummary
  instructions.value = draft.instructions
  yieldQuantity.value = draft.yieldQuantity
  yieldUnit.value = draft.yieldUnit
  if (draft.ingredients.length) ingredients.value = draft.ingredients.map((item) => ({ ...item }))
  tags.value = [...(draft.tags ?? [])]
  // Se o rascunho já tem conteúdo próprio de receita, não deixa o watcher de
  // recipeQuery sobrescrever com a receita original de novo.
  if (draft.instructions.trim() || draft.ingredients.some((item) => item.name.trim())) {
    prefilled.value = true
  }
  if (draft.image) {
    image.value = draft.image
    imagePreview.value = URL.createObjectURL(draft.image)
  }
  lastDraftSavedAt.value = draft.updatedAt
}

onMounted(async () => {
  if (routeDraftId.value) await loadDraft(routeDraftId.value)
  existingDraftsCount.value = (await listDrafts()).length
  autosaveTimer = setInterval(() => void autosaveDraft(), 10_000)
})

const createMutation = useCreateUpload({
  mutation: {
    onSuccess: (result) => finish(result.id),
    onError: (error) => handleMutationError(error, 'Não foi possível publicar.'),
  },
})
const versionMutation = useCreateMyVersion({
  mutation: {
    onSuccess: (result) => finish(result.id),
    onError: (error) => handleMutationError(error, 'Não foi possível publicar sua versão.'),
  },
})
const isSubmitting = computed(
  () => createMutation.isPending.value || versionMutation.isPending.value,
)
const recipeMode = computed(() => isMyVersion.value || type.value === 'RECIPE')
const pageTitle = computed(() => (isMyVersion.value ? 'Publicar minha versão' : 'O que você fez?'))
const submitLabel = computed(() => (isMyVersion.value ? 'Publicar minha versão' : 'Publicar'))

const ACCEPTED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/heic',
  'image/heif',
]
const ACCEPTED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif']

function hasAcceptedExtension(filename: string): boolean {
  const lower = filename.toLowerCase()
  return ACCEPTED_IMAGE_EXTENSIONS.some((extension) => lower.endsWith(extension))
}

function acceptImage(event: Event): void {
  const selected = (event.target as HTMLInputElement).files?.[0] ?? null
  imageError.value = ''
  image.value = null
  photoRejected.value = false
  photoFeedbackOpen.value = false
  photoFeedbackSent.value = false
  photoFeedbackComment.value = ''
  clearImagePreview()
  if (!selected) return
  // Alguns navegadores (ex.: Safari com arquivos HEIC) não preenchem `type`,
  // então também aceitamos com base na extensão do arquivo.
  if (!ACCEPTED_IMAGE_TYPES.includes(selected.type) && !hasAcceptedExtension(selected.name)) {
    imageError.value = 'Escolha uma imagem JPEG, PNG, WebP, HEIC ou HEIF.'
    return
  }
  if (selected.size > 20 * 1024 * 1024) {
    imageError.value = 'A imagem deve ter no máximo 20 MB.'
    return
  }
  image.value = selected
  imagePreview.value = URL.createObjectURL(selected)
}

function clearImagePreview(): void {
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
  imagePreview.value = ''
}

function clearErrors(): void {
  formError.value = ''
  ingredientError.value = ''
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
}

// Canal mínimo de "acha que erramos" (impl10.md v10 §21.2): a recusa é definitiva,
// não existe publicar assim mesmo — isto é o jeito de descobrir falsos negativos,
// já que não existe monitoramento automático do classificador.
const photoRejected = ref(false)
const photoFeedbackOpen = ref(false)
const photoFeedbackComment = ref('')
const photoFeedbackSent = ref(false)
const photoFeedbackMutation = useReportPhotoValidationFeedback({
  mutation: {
    onSuccess: () => {
      photoFeedbackSent.value = true
      photoFeedbackOpen.value = false
    },
  },
})

function submitPhotoFeedback(): void {
  photoFeedbackMutation.mutate({
    data: {
      reasonCode: 'IMAGE_NOT_FOOD',
      ...(photoFeedbackComment.value.trim() ? { comment: photoFeedbackComment.value } : {}),
    },
  })
}

function handleMutationError(error: unknown, fallback: string): void {
  const normalized = normalizeHttpError(error)
  Object.assign(fieldErrors, normalized.fieldErrors)
  imageError.value = normalized.fieldErrors.image ?? imageError.value
  ingredientError.value =
    normalized.fieldErrors.ingredients ??
    normalized.fieldErrors['recipe.ingredients'] ??
    ingredientError.value
  photoRejected.value = normalized.code === 'IMAGE_NOT_FOOD'
  formError.value = photoRejected.value
    ? 'Não reconhecemos comida nesta foto. Escolha outra para continuar.'
    : `${normalized.message || fallback} Seus dados foram preservados.`
  if (normalized.code === 'RATE_LIMIT_EXCEEDED' && normalized.nextAvailableAt) {
    startRateLimitCooldown(normalized.nextAvailableAt)
  }
}

function recipePayload(): CreateRecipeRequest | undefined {
  if (!recipeMode.value) return undefined
  const valid = ingredients.value.filter((item) => item.name.trim())
  if (!valid.length) {
    ingredientError.value = 'Adicione pelo menos um ingrediente.'
    return undefined
  }
  if (!instructions.value.trim()) {
    formError.value = 'Informe o modo de preparo.'
    return undefined
  }
  ingredientError.value = ''
  return {
    instructions: instructions.value,
    ingredients: valid.map((item, index) => ({
      position: index + 1,
      name: item.name,
      ...(item.quantity.trim() ? { quantity: Number(item.quantity) } : {}),
      ...(item.unit.trim() ? { unit: item.unit } : {}),
      ...(item.note.trim() ? { note: item.note } : {}),
    })),
    ...(yieldQuantity.value ? { yieldQuantity: Number(yieldQuantity.value) } : {}),
    ...(yieldUnit.value.trim() ? { yieldUnit: yieldUnit.value } : {}),
  }
}

function submit(): void {
  if (isSubmitting.value || isRateLimited.value) return
  clearErrors()
  if (!image.value) {
    imageError.value = 'Selecione uma imagem para publicar.'
    return
  }
  if (isMyVersion.value && !titleSuffix.value.trim()) {
    formError.value = 'Informe o sufixo do título da sua versão.'
    return
  }
  const recipe = recipePayload()
  if (recipeMode.value && !recipe) return
  if (isMyVersion.value) {
    versionMutation.mutate({
      id: sourceId.value,
      data: {
        data: {
          visibility: visibility.value,
          titleSuffix: titleSuffix.value,
          ...(changeSummary.value.trim() ? { changeSummary: changeSummary.value } : {}),
          recipe: recipe!,
          ...(tags.value.length ? { tags: tags.value } : {}),
        },
        image: image.value,
      },
    })
  } else {
    createMutation.mutate({
      data: {
        data: {
          type: type.value,
          visibility: visibility.value,
          ...(title.value.trim() ? { title: title.value } : {}),
          ...(description.value.trim() ? { description: description.value } : {}),
          ...(recipe ? { recipe } : {}),
          ...(tags.value.length ? { tags: tags.value } : {}),
        },
        image: image.value,
      },
    })
  }
}

function finish(id: string): void {
  published.value = true
  publishedId.value = id
  clearImagePreview()
  if (autosaveTimer) clearInterval(autosaveTimer)
  void deleteDraft(draftId.value)
  queryClient.invalidateQueries({ queryKey: ['publications'] })
}

function goToFeed(): void {
  void router.push('/')
}

function publishAnother(): void {
  published.value = false
  publishedId.value = ''
  type.value = 'DISH'
  visibility.value = 'PUBLIC'
  title.value = ''
  description.value = ''
  changeSummary.value = ''
  instructions.value = ''
  yieldQuantity.value = ''
  yieldUnit.value = ''
  image.value = null
  clearImagePreview()
  ingredients.value = [{ name: '', quantity: '', unit: '', note: '' }]
  tags.value = []
  clearErrors()
  draftId.value = crypto.randomUUID()
  draftCreatedAt = new Date().toISOString()
  lastDraftSavedAt.value = null
  autosaveTimer = setInterval(() => void autosaveDraft(), 10_000)
}

const prefilled = ref(false)

watch(recipeQuery.data, (recipe) => {
  if (!recipe || prefilled.value) return
  prefilled.value = true
  ingredients.value = recipe.ingredients.map((item) => ({
    name: item.name,
    quantity: item.quantity == null ? '' : String(item.quantity),
    unit: item.unit ?? '',
    note: item.note ?? '',
  }))
  instructions.value = recipe.instructions
  yieldQuantity.value = recipe.yieldQuantity == null ? '' : String(recipe.yieldQuantity)
  yieldUnit.value = recipe.yieldUnit ?? ''
})

const sourceTitle = computed(() => sourceQuery.data.value?.title ?? 'receita original')
const titlePreview = computed(() => `${sourceTitle.value} · ${titleSuffix.value.trim() || 'sua versão'}`)
const sourceLoadError = computed(() => {
  const error = sourceQuery.error.value ?? recipeQuery.error.value
  return isMyVersion.value && error ? normalizeHttpError(error).message : null
})

onBeforeUnmount(() => {
  clearImagePreview()
  if (autosaveTimer) clearInterval(autosaveTimer)
  if (cooldownTicker) clearInterval(cooldownTicker)
  if (!published.value) void autosaveDraft()
})
</script>

<template>
  <article class="create-publication">
    <section v-if="published" class="create-publication__done">
      <StatusRing variant="success" />
      <h1>Publicado!</h1>
      <p>Sua publicação já está sendo validada e vai aparecer no feed em instantes.</p>
      <div class="create-publication__done-actions">
        <BaseButton @click="goToFeed">Ver no feed</BaseButton>
        <BaseButton variant="secondary" @click="publishAnother">Publicar outra coisa</BaseButton>
      </div>
    </section>

    <template v-else>
      <RouterLink class="create-publication__back" to="/">
        <AppIcon name="back" :size="18" :stroke-width="2" />
        Voltar
      </RouterLink>

      <h1>{{ pageTitle }}</h1>
      <p v-if="isMyVersion" class="create-publication__intro">
        Já trouxemos a receita — mude o que você fez diferente, o resto pode ficar como está.
      </p>
      <p v-else class="create-publication__intro">
        Uma foto e duas linhas já bastam. Não precisa ser receita completa.
      </p>

      <RouterLink
        v-if="isMyVersion && sourceQuery.data.value"
        class="create-publication__origin"
        :to="`/publicacoes/${sourceId}`"
      >
        <img
          v-if="sourceQuery.data.value.imageUrl"
          class="create-publication__origin-thumb"
          :src="resolveImageUrl(sourceQuery.data.value.imageUrl)"
          alt=""
        />
        <span>
          <strong>{{ sourceTitle }}</strong>
          <span class="create-publication__origin-meta">
            de {{ sourceQuery.data.value.authorDisplayName }} · a receita original
          </span>
        </span>
        <span class="create-publication__origin-link">Ver original</span>
      </RouterLink>

      <div v-if="sourceLoadError" class="create-publication__source-error" role="alert">
        <strong>Não foi possível carregar a receita original.</strong>
        <p>{{ sourceLoadError }}</p>
        <BaseButton variant="secondary" @click="router.back()">Voltar</BaseButton>
      </div>
      <form v-else class="create-publication__form" novalidate @submit.prevent="submit">
        <p v-if="lastDraftSavedAt || existingDraftsCount" class="create-publication__draft-status">
          <AppIcon name="check" :size="15" :stroke-width="2.2" />
          <span v-if="lastDraftSavedAt">Rascunho salvo às {{ formatSavedTime(lastDraftSavedAt) }}. </span>
          <RouterLink to="/rascunhos"
            >Ver rascunhos salvos<span v-if="existingDraftsCount"> ({{ existingDraftsCount }})</span></RouterLink
          >
        </p>

        <fieldset class="create-publication__image">
          <legend>Foto <span aria-hidden="true">*</span></legend>
          <label
            class="create-publication__photo"
            :class="{ 'create-publication__photo--filled': imagePreview }"
          >
            <input
              type="file"
              class="create-publication__photo-input"
              accept="image/jpeg,image/png,image/webp,image/heic,image/heif,.heic,.heif"
              required
              @change="acceptImage"
            />
            <template v-if="!imagePreview">
              <AppIcon name="camera" :size="32" :stroke-width="1.5" />
              <span class="create-publication__photo-cta">Comece pela foto</span>
              <span class="create-publication__photo-hint">
                JPEG, PNG, WebP, HEIC ou HEIF até 20 MB.
              </span>
            </template>
            <template v-else>
              <img :src="imagePreview" alt="Prévia da imagem selecionada" />
              <span class="create-publication__photo-swap">Trocar foto</span>
            </template>
          </label>
          <p v-if="imagePreview" class="create-publication__photo-note">
            Depois de publicar, ela não poderá mais ser trocada.
          </p>
        </fieldset>

        <BaseSelect v-if="!isMyVersion" v-model="type" label="Tipo de publicação" required>
          <option value="DISH">Prato</option>
          <option value="RECIPE">Receita</option>
        </BaseSelect>

        <div v-if="isMyVersion" class="create-publication__title-box">
          <label class="create-publication__title-label" for="my-version-suffix">
            Como você vai chamar a sua? <span aria-hidden="true">*</span>
          </label>
          <div class="create-publication__title-control">
            <span class="create-publication__title-fixed">{{ sourceTitle }} ·</span>
            <input
              id="my-version-suffix"
              v-model="titleSuffix"
              type="text"
              maxlength="100"
              placeholder="do meu jeito"
            />
          </div>
          <BaseFieldError v-if="fieldErrors.titleSuffix" :message="fieldErrors.titleSuffix" />
          <p class="create-publication__title-preview">
            Vai aparecer como: <strong>{{ titlePreview }}</strong>
          </p>
        </div>
        <BaseInput
          v-else
          v-model="title"
          label="Como se chama?"
          hint="Opcional, até 150 caracteres."
          maxlength="150"
          :error="fieldErrors.title"
        />

        <BaseTextarea
          v-if="!isMyVersion && type === 'DISH'"
          v-model="description"
          label="Conte alguma coisa"
          hint="Opcional, até 2.000 caracteres."
          maxlength="2000"
          :error="fieldErrors.description"
        />
        <BaseTextarea
          v-if="isMyVersion"
          v-model="changeSummary"
          label="O que você mudou?"
          hint="Opcional, até 2.000 caracteres."
          maxlength="2000"
          :error="fieldErrors.changeSummary"
        />

        <div v-if="recipeMode" class="create-publication__recipe">
          <IngredientEditor v-model="ingredients" :error="ingredientError" /><PreparationStepsEditor
            v-model="instructions"
            :error="fieldErrors.instructions ?? fieldErrors['recipe.instructions']"
          />
          <div class="create-publication__yield">
            <BaseInput v-model="yieldQuantity" label="Rendimento" type="number" :min="0" /><BaseInput
              v-model="yieldUnit"
              label="Unidade do rendimento"
              placeholder="porções, fatias..."
              maxlength="50"
            />
          </div>
        </div>

        <TagEditor v-model="tags" :error="fieldErrors.tags" />

        <div class="create-publication__visibility">
          <span class="create-publication__visibility-label">Quem pode ver</span>
          <RadioCardGroup v-model="visibility" :options="VISIBILITY_OPTIONS" />
        </div>

        <BaseFieldError v-if="imageError" :message="imageError" /><BaseFieldError
          v-if="formError"
          :message="formError"
        />

        <div v-if="photoRejected" class="create-publication__photo-feedback">
          <p v-if="photoFeedbackSent">Obrigado! Seu relato foi enviado.</p>
          <template v-else-if="photoFeedbackOpen">
            <BaseTextarea
              v-model="photoFeedbackComment"
              label="Conte pra gente"
              hint="Opcional. Ajuda a melhorar o reconhecimento de fotos."
              maxlength="1000"
            />
            <BaseButton
              type="button"
              variant="secondary"
              :loading="photoFeedbackMutation.isPending.value"
              @click="submitPhotoFeedback"
            >
              Enviar relato
            </BaseButton>
          </template>
          <button
            v-else
            type="button"
            class="create-publication__photo-feedback-link"
            @click="photoFeedbackOpen = true"
          >
            Acha que erramos? Conte pra gente
          </button>
        </div>

        <BaseToast
          v-if="isRateLimited"
          kind="warning"
          title="Limite de publicações atingido"
          :dismissible="false"
        >
          Tente novamente em {{ cooldownSecondsRemaining }}
          {{ cooldownSecondsRemaining === 1 ? 'segundo' : 'segundos' }}.
        </BaseToast>

        <div class="create-publication__actions">
          <BaseButton
            type="button"
            variant="secondary"
            :disabled="isSubmitting"
            @click="router.back()"
          >
            Cancelar
          </BaseButton>
          <BaseButton type="submit" :loading="isSubmitting" :disabled="isRateLimited">
            {{ isRateLimited ? `Aguarde ${cooldownSecondsRemaining}s` : submitLabel }}
          </BaseButton>
        </div>
      </form>
    </template>
  </article>
</template>

<style scoped>
.create-publication {
  max-width: 40rem;
  margin-inline: auto;
}

.create-publication__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-end: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.create-publication__back:hover {
  color: var(--color-primary);
}

.create-publication h1 {
  margin: 0;
  font-size: clamp(2rem, 5vw, 2.5rem);
}

.create-publication__intro {
  margin-block: var(--space-2) var(--space-8);
  color: var(--color-text-secondary);
}

.create-publication__origin {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  margin-block-end: var(--space-6);
  color: inherit;
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.create-publication__origin-thumb {
  width: 3rem;
  height: 3rem;
  flex: none;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.create-publication__origin strong {
  display: block;
}

.create-publication__origin-meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.create-publication__origin-link {
  margin-inline-start: auto;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.create-publication__form {
  display: grid;
  gap: var(--space-6);
}

.create-publication__draft-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.create-publication__source-error {
  display: grid;
  gap: var(--space-3);
  margin-block-start: var(--space-8);
  padding: var(--space-6);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-md);
}

.create-publication__source-error p {
  margin: 0;
}

.create-publication__source-error .base-button {
  width: fit-content;
}

.create-publication__image {
  display: grid;
  gap: var(--space-2);
  padding: 0;
  border: 0;
}

.create-publication__image legend {
  margin-block-end: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.create-publication__image legend span {
  color: var(--color-danger);
}

.create-publication__photo {
  position: relative;
  display: grid;
  aspect-ratio: 4 / 3;
  justify-items: center;
  gap: var(--space-2);
  padding: var(--space-6);
  overflow: hidden;
  color: var(--color-text-secondary);
  text-align: center;
  cursor: pointer;
  background: var(--color-surface);
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-lg);
  place-content: center;
}

.create-publication__photo:hover {
  border-color: var(--color-primary);
}

.create-publication__photo--filled {
  border-style: solid;
  padding: 0;
}

.create-publication__photo-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

.create-publication__photo-cta {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.create-publication__photo-hint {
  font-size: var(--font-size-sm);
}

.create-publication__photo--filled img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.create-publication__photo-swap {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-3);
  padding: var(--space-2) var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: var(--color-surface-raised);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
}

.create-publication__photo-note {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.create-publication__title-box {
  display: grid;
  gap: var(--space-2);
}

.create-publication__title-label {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.create-publication__title-control {
  display: flex;
  align-items: center;
  min-height: var(--control-min-size);
  padding-inline-start: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.create-publication__title-fixed {
  flex: none;
  padding-inline-end: var(--space-1);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.create-publication__title-control input {
  width: 100%;
  min-height: var(--control-min-size);
  padding-inline: var(--space-1) var(--space-4);
  color: var(--color-text);
  background: transparent;
  border: 0;
}

.create-publication__title-preview {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.create-publication__title-preview strong {
  color: var(--color-text);
}

.create-publication__recipe {
  display: grid;
  gap: var(--space-6);
}

.create-publication__yield {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.create-publication__visibility {
  display: grid;
  gap: var(--space-3);
}

.create-publication__visibility-label {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.create-publication__actions {
  display: flex;
  flex-wrap: wrap-reverse;
  justify-content: flex-end;
  gap: var(--space-3);
}

.create-publication__photo-feedback {
  display: grid;
  gap: var(--space-3);
}

.create-publication__photo-feedback-link {
  width: fit-content;
  padding: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: underline;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.create-publication__done {
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  padding: var(--space-12) var(--space-6);
  text-align: center;
}

.create-publication__done p {
  max-width: 26rem;
  margin: 0;
  color: var(--color-text-secondary);
}

.create-publication__done-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-3);
  margin-block-start: var(--space-4);
}

@media (max-width: 48rem) {
  .create-publication__yield {
    grid-template-columns: 1fr;
  }
}
</style>
