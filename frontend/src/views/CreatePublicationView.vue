<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import {
  useCreateMyVersion,
  useCreateUpload,
  useGetPublicationById,
  useGetPublicationRecipe,
} from '@/api/generated/publications/publications'
import type { CreateRecipeRequest } from '@/api/generated/models'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import IngredientEditor, {
  type IngredientDraft,
} from '@/components/publication/IngredientEditor.vue'
import PreparationStepsEditor from '@/components/publication/PreparationStepsEditor.vue'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const sourceId = computed(() => (route.params.sourceId as string | undefined) ?? '')
const isMyVersion = computed(() => Boolean(sourceId.value))
const sourceQuery = useGetPublicationById(sourceId, {
  query: { enabled: computed(() => isMyVersion.value) },
})
const recipeQuery = useGetPublicationRecipe(sourceId, {
  query: { enabled: computed(() => isMyVersion.value) },
})

const type = ref<'DISH' | 'RECIPE'>('DISH')
const visibility = ref<'PUBLIC' | 'INTERNAL'>('PUBLIC')
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
const ingredients = ref<IngredientDraft[]>([{ name: '', quantity: '', unit: '', note: '' }])

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
const pageTitle = computed(() => (isMyVersion.value ? 'Publicar minha versão' : 'Criar publicação'))

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
  clearImagePreview()
  if (!selected) return
  // Alguns navegadores (ex.: Safari com arquivos HEIC) não preenchem `type`,
  // então também aceitamos com base na extensão do arquivo.
  if (!ACCEPTED_IMAGE_TYPES.includes(selected.type) && !hasAcceptedExtension(selected.name)) {
    imageError.value = 'Escolha uma imagem JPEG, PNG, WebP, HEIC ou HEIF.'
    return
  }
  if (selected.size > 15 * 1024 * 1024) {
    imageError.value = 'A imagem deve ter no máximo 15 MB.'
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

function handleMutationError(error: unknown, fallback: string): void {
  const normalized = normalizeHttpError(error)
  Object.assign(fieldErrors, normalized.fieldErrors)
  imageError.value = normalized.fieldErrors.image ?? imageError.value
  ingredientError.value =
    normalized.fieldErrors.ingredients ??
    normalized.fieldErrors['recipe.ingredients'] ??
    ingredientError.value
  formError.value = `${normalized.message || fallback} Seus dados foram preservados.`
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
  if (isSubmitting.value) return
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
        },
        image: image.value,
      },
    })
  }
}

function finish(id: string): void {
  published.value = true
  clearImagePreview()
  queryClient.invalidateQueries({ queryKey: ['publications'] })
  void router.push(`/publicacoes/${id}`)
}

function useOriginalRecipe(): void {
  const recipe = recipeQuery.data.value
  if (!recipe) return
  ingredients.value = recipe.ingredients.map((item) => ({
    name: item.name,
    quantity: item.quantity == null ? '' : String(item.quantity),
    unit: item.unit ?? '',
    note: item.note ?? '',
  }))
  instructions.value = recipe.instructions
  yieldQuantity.value = recipe.yieldQuantity == null ? '' : String(recipe.yieldQuantity)
  yieldUnit.value = recipe.yieldUnit ?? ''
  if (!title.value) title.value = sourceQuery.data.value?.title ?? ''
}

const sourceTitle = computed(() => sourceQuery.data.value?.title ?? 'receita original')
const sourceLoadError = computed(() => {
  const error = sourceQuery.error.value ?? recipeQuery.error.value
  return isMyVersion.value && error ? normalizeHttpError(error).message : null
})

onBeforeUnmount(clearImagePreview)
</script>

<template>
  <article class="create-publication">
    <p class="create-publication__eyebrow">Publicação</p>
    <h1>{{ pageTitle }}</h1>
    <p v-if="isMyVersion" class="create-publication__intro">
      Crie sua própria versão de <strong>{{ sourceTitle }}</strong
      >.
    </p>
    <div v-if="sourceLoadError" class="create-publication__source-error" role="alert">
      <strong>Não foi possível carregar a receita original.</strong>
      <p>{{ sourceLoadError }}</p>
      <BaseButton variant="secondary" @click="router.back()">Voltar</BaseButton>
    </div>
    <form v-else class="create-publication__form" novalidate @submit.prevent="submit">
      <section v-if="isMyVersion" class="create-publication__source">
        <p>Os ingredientes da publicação original não são alterados.</p>
        <button
          type="button"
          :disabled="recipeQuery.isPending.value || !recipeQuery.data.value"
          @click="useOriginalRecipe"
        >
          {{
            recipeQuery.isPending.value
              ? 'Carregando receita original…'
              : 'Usar receita original como ponto de partida'
          }}
        </button>
      </section>
      <BaseSelect v-if="!isMyVersion" v-model="type" label="Tipo de publicação" required>
        <option value="DISH">Prato</option>
        <option value="RECIPE">Receita</option>
      </BaseSelect>
      <BaseInput
        v-if="isMyVersion"
        v-model="titleSuffix"
        label="Sufixo do título"
        hint="Será acrescentado ao título da publicação original."
        maxlength="100"
        :error="fieldErrors.titleSuffix"
        required
      />
      <BaseInput
        v-else
        v-model="title"
        label="Título"
        hint="Opcional, até 150 caracteres."
        maxlength="150"
        :error="fieldErrors.title"
      />
      <BaseTextarea
        v-if="!isMyVersion && type === 'DISH'"
        v-model="description"
        label="Descrição"
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
      <fieldset class="create-publication__image">
        <legend>Imagem <span aria-hidden="true">*</span></legend>
        <label class="create-publication__file"
          >Escolher imagem<input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/heic,image/heif,.heic,.heif"
            required
            @change="acceptImage"
        /></label>
        <p>JPEG, PNG, WebP, HEIC ou HEIF até 15 MB.</p>
        <img v-if="imagePreview" :src="imagePreview" alt="Prévia da imagem selecionada" />
      </fieldset>
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
      <BaseSelect
        v-model="visibility"
        label="Visibilidade"
        required
        :hint="
          visibility === 'PUBLIC'
            ? 'Pública: qualquer pessoa pode ver, mesmo sem conta no Comes&Bebes.'
            : 'Interna: só é vista por quem tem conta e está conectado ao Comes&Bebes.'
        "
        ><option value="PUBLIC">Pública</option>
        <option value="INTERNAL">Interna</option></BaseSelect
      >
      <BaseFieldError v-if="imageError" :message="imageError" /><BaseFieldError
        v-if="formError"
        :message="formError"
      />
      <div class="create-publication__actions">
        <BaseButton type="submit" :loading="isSubmitting">Publicar</BaseButton
        ><BaseButton
          type="button"
          variant="secondary"
          :disabled="isSubmitting"
          @click="router.back()"
          >Cancelar</BaseButton
        >
      </div>
      <p v-if="published" class="create-publication__status" role="status">
        A imagem será validada antes da publicação aparecer no feed.
      </p>
    </form>
  </article>
</template>

<style scoped>
.create-publication {
  max-width: 50rem;
  margin-inline: auto;
}
.create-publication__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}
.create-publication h1 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: clamp(2rem, 5vw, 3.5rem);
}
.create-publication__intro {
  margin-block: var(--space-3) var(--space-8);
  color: var(--color-text-secondary);
}
.create-publication__form {
  display: grid;
  gap: var(--space-6);
  margin-block-start: var(--space-8);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.create-publication__source {
  padding: var(--space-4);
  color: var(--color-text-secondary);
  background: var(--color-surface-raised);
  border-inline-start: 3px solid var(--color-primary);
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
.create-publication__source p {
  margin: 0 0 var(--space-2);
}
.create-publication__source button {
  padding: 0;
  color: var(--color-primary);
  text-decoration: underline;
  background: none;
  border: 0;
}
.create-publication__source button:disabled {
  opacity: 0.6;
  cursor: wait;
}
.create-publication__image {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  border: 0;
}
.create-publication__image legend {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}
.create-publication__image legend span {
  color: var(--color-danger);
}
.create-publication__file {
  display: inline-flex;
  width: fit-content;
  min-height: var(--control-min-size);
  align-items: center;
  padding: var(--space-3) var(--space-5);
  color: var(--color-primary-contrast);
  font-weight: var(--font-weight-semibold);
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.create-publication__file input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
.create-publication__image > p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
.create-publication__image img {
  width: min(100%, 18rem);
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: var(--radius-sm);
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
.create-publication__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}
.create-publication__status {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
@media (max-width: 48rem) {
  .create-publication__form {
    padding: var(--space-5);
  }
  .create-publication__yield {
    grid-template-columns: 1fr;
  }
}
</style>
