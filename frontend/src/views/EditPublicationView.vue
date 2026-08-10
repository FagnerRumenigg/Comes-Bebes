<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import {
  useGetPublicationById,
  useGetPublicationRecipe,
  useUpdatePublication,
} from '@/api/generated/publications/publications'
import type { CreateRecipeRequest, UpdatePublicationRequest } from '@/api/generated/models'
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
import PublicationImage from '@/components/publication/PublicationImage.vue'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()
const publicationId = computed(() => String(route.params.id))
const publicationQuery = useGetPublicationById(publicationId)
const hasRecipe = computed(() => {
  const type = publicationQuery.data.value?.type
  return type === 'RECIPE' || type === 'MY_VERSION'
})
const recipeQuery = useGetPublicationRecipe(publicationId, {
  query: { enabled: computed(() => Boolean(publicationQuery.data.value && hasRecipe.value)) },
})

const title = ref('')
const description = ref('')
const visibility = ref<'PUBLIC' | 'INTERNAL'>('PUBLIC')
const instructions = ref('')
const yieldQuantity = ref('')
const yieldUnit = ref('')
const ingredients = ref<IngredientDraft[]>([{ name: '', quantity: '', unit: '', note: '' }])
const initialized = ref(false)
const formError = ref('')
const ingredientError = ref('')
const fieldErrors = reactive<Record<string, string>>({})

const isOwner = computed(
  () =>
    Boolean(publicationQuery.data.value) &&
    authStore.identity?.userId === publicationQuery.data.value?.authorId,
)
const isLoading = computed(
  () =>
    publicationQuery.isPending.value ||
    (hasRecipe.value && recipeQuery.isPending.value) ||
    (!initialized.value && !publicationQuery.isError.value),
)
const loadError = computed(() => {
  const error = publicationQuery.error.value ?? recipeQuery.error.value
  return error ? normalizeHttpError(error).message : null
})

watch(
  [() => publicationQuery.data.value, () => recipeQuery.data.value],
  ([publication, recipe]) => {
    if (!publication || initialized.value || (hasRecipe.value && !recipe)) return
    title.value = publication.title ?? ''
    description.value = publication.description ?? ''
    visibility.value = publication.visibility
    if (recipe) {
      instructions.value = recipe.instructions
      yieldQuantity.value = recipe.yieldQuantity == null ? '' : String(recipe.yieldQuantity)
      yieldUnit.value = recipe.yieldUnit ?? ''
      ingredients.value = recipe.ingredients.map((item) => ({
        name: item.name,
        quantity: item.quantity == null ? '' : String(item.quantity),
        unit: item.unit ?? '',
        note: item.note ?? '',
      }))
    }
    initialized.value = true
  },
  { immediate: true },
)

const updateMutation = useUpdatePublication({
  mutation: {
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['publications'] })
      void router.push(`/publicacoes/${publicationId.value}`)
    },
    onError: (error) => {
      const normalized = normalizeHttpError(error)
      Object.assign(fieldErrors, normalized.fieldErrors)
      ingredientError.value =
        normalized.fieldErrors.ingredients ??
        normalized.fieldErrors['recipe.ingredients'] ??
        ingredientError.value
      formError.value = `${normalized.message} Seus dados foram preservados.`
    },
  },
})

function clearErrors(): void {
  formError.value = ''
  ingredientError.value = ''
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
}

function recipePayload(): CreateRecipeRequest | undefined {
  if (!hasRecipe.value) return undefined
  const validIngredients = ingredients.value.filter((item) => item.name.trim())
  if (!validIngredients.length) {
    ingredientError.value = 'Adicione pelo menos um ingrediente.'
    return undefined
  }
  if (!instructions.value.trim()) {
    formError.value = 'Informe pelo menos um passo do modo de preparo.'
    return undefined
  }
  return {
    instructions: instructions.value,
    ingredients: validIngredients.map((item, index) => ({
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
  if (updateMutation.isPending.value || !isOwner.value) return
  clearErrors()
  if (hasRecipe.value && !title.value.trim()) {
    formError.value = 'Informe o título da receita.'
    return
  }
  const recipe = recipePayload()
  if (hasRecipe.value && !recipe) return

  const data: UpdatePublicationRequest = {
    title: title.value,
    description: description.value,
    visibility: visibility.value,
    ...(recipe ? { recipe } : {}),
  }
  updateMutation.mutate({ id: publicationId.value, data })
}
</script>

<template>
  <article class="edit-publication">
    <p class="edit-publication__eyebrow">Sua publicação</p>
    <h1>Editar publicação</h1>

    <div v-if="isLoading" class="edit-publication__state" role="status">Carregando publicação…</div>
    <div v-else-if="loadError" class="edit-publication__state" role="alert">
      <strong>Não foi possível carregar a publicação.</strong>
      <p>{{ loadError }}</p>
      <BaseButton variant="secondary" @click="router.back()">Voltar</BaseButton>
    </div>
    <div v-else-if="!isOwner" class="edit-publication__state" role="alert">
      <strong>Essa publicação não pertence a você.</strong>
      <p>Somente o autor pode fazer alterações.</p>
      <BaseButton variant="secondary" @click="router.back()">Voltar</BaseButton>
    </div>
    <form v-else class="edit-publication__form" novalidate @submit.prevent="submit">
      <BaseInput
        v-model="title"
        label="Título"
        maxlength="150"
        :required="hasRecipe"
        :error="fieldErrors.title"
      />
      <BaseTextarea
        v-model="description"
        label="Descrição"
        hint="Opcional, até 2.000 caracteres."
        maxlength="2000"
        :error="fieldErrors.description"
      />

      <section v-if="publicationQuery.data.value" class="edit-publication__image">
        <div>
          <strong>Imagem atual</strong>
          <p>A imagem não é alterada durante a edição.</p>
        </div>
        <PublicationImage
          :src="publicationQuery.data.value.imageUrl"
          :alt="publicationQuery.data.value.title ?? 'Imagem da publicação'"
        />
      </section>

      <div v-if="hasRecipe" class="edit-publication__recipe">
        <IngredientEditor v-model="ingredients" :error="ingredientError" />
        <PreparationStepsEditor
          v-model="instructions"
          :error="fieldErrors.instructions ?? fieldErrors['recipe.instructions']"
        />
        <div class="edit-publication__yield">
          <BaseInput v-model="yieldQuantity" label="Rendimento" type="number" :min="0" />
          <BaseInput
            v-model="yieldUnit"
            label="Unidade do rendimento"
            placeholder="porções, fatias..."
            maxlength="50"
          />
        </div>
      </div>

      <BaseSelect v-model="visibility" label="Visibilidade" required>
        <option value="PUBLIC">Pública</option>
        <option value="INTERNAL">Interna</option>
      </BaseSelect>
      <BaseFieldError v-if="formError" :message="formError" />
      <div class="edit-publication__actions">
        <BaseButton type="submit" :loading="updateMutation.isPending.value">
          Salvar alterações
        </BaseButton>
        <BaseButton
          type="button"
          variant="secondary"
          :disabled="updateMutation.isPending.value"
          @click="router.back()"
        >
          Cancelar
        </BaseButton>
      </div>
    </form>
  </article>
</template>

<style scoped>
.edit-publication {
  max-width: 50rem;
  margin-inline: auto;
}

.edit-publication__eyebrow {
  margin-block-end: var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.edit-publication h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
}

.edit-publication__form {
  display: grid;
  gap: var(--space-6);
  margin-block-start: var(--space-8);
  padding: var(--space-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.edit-publication__state {
  display: grid;
  min-height: 20rem;
  place-content: center;
  justify-items: center;
  gap: var(--space-3);
  color: var(--color-text-secondary);
  text-align: center;
}

.edit-publication__state p,
.edit-publication__image p {
  margin: 0;
}

.edit-publication__image {
  display: grid;
  grid-template-columns: 1fr 8rem;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface-raised);
  border-radius: var(--radius-md);
}

.edit-publication__image :deep(.publication-image) {
  aspect-ratio: 4 / 5;
  border-radius: var(--radius-sm);
}

.edit-publication__image p {
  margin-block-start: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.edit-publication__recipe {
  display: grid;
  gap: var(--space-6);
}

.edit-publication__yield {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.edit-publication__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

@media (max-width: 48rem) {
  .edit-publication__form {
    padding: var(--space-5);
  }

  .edit-publication__yield {
    grid-template-columns: 1fr;
  }
}
</style>
