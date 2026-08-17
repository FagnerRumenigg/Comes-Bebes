import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'

import { setAccessTokenProvider } from '@/api/client'
import CreatePublicationView from '@/views/CreatePublicationView.vue'
import { useAuthStore } from '@/stores/auth.store'
import { mockPublications } from '@/mocks/fixtures/publications'

async function mountMyVersion() {
  const source = mockPublications[1]!

  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  authStore.acceptSession({
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresInSeconds: 3600,
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    userId: '71131447-a2a0-4996-a336-a8c3555bb327',
    username: 'fagner',
    role: 'USER',
    onboardingCompleted: true,
    hasUnseenPatchNotes: false,
    sessionId: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
    deviceId: 'e1f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
  })
  setAccessTokenProvider(() => authStore.accessToken)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/publicar/minha-versao/:sourceId', component: CreatePublicationView },
      { path: '/publicacoes/:id', component: { template: '<div>Detalhes</div>' } },
    ],
  })
  await router.push(`/publicar/minha-versao/${source.id}`)
  await router.isReady()

  const wrapper = mount(CreatePublicationView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })

  return { wrapper, source }
}

describe('publicar minha versão', () => {
  it('pré-carrega a receita original automaticamente, sem exigir clique', async () => {
    const { wrapper, source } = await mountMyVersion()
    const recipe = source.recipePreview!

    expect(wrapper.get('h1').text()).toBe('Publicar minha versão')

    await vi.waitFor(() => {
      const firstIngredientInput = wrapper.find('.ingredient-editor__row input')
      expect(firstIngredientInput.element).toHaveProperty('value', 'cenoura')
    })

    expect(wrapper.text()).toContain(source.title)

    const stepTextareas = wrapper.findAll('.preparation-steps__row textarea')
    const joinedInstructions = stepTextareas
      .map((textarea) => (textarea.element as HTMLTextAreaElement).value)
      .join('\n')
    expect(joinedInstructions).toBe(recipe.instructions)

    const yieldInputs = wrapper.findAll('.create-publication__yield input')
    expect((yieldInputs[0]!.element as HTMLInputElement).value).toBe(String(recipe.yieldQuantity))
    expect((yieldInputs[1]!.element as HTMLInputElement).value).toBe(recipe.yieldUnit)

    expect(wrapper.text()).toContain(
      'Começamos com a receita original para facilitar. Ajuste os ingredientes, o preparo e o rendimento para mostrar como você fez a sua versão.',
    )
  })

  it('não mostra mais o antigo botão manual "Usar receita original"', async () => {
    const { wrapper } = await mountMyVersion()

    await vi.waitFor(() => {
      expect(wrapper.find('.ingredient-editor__row input').element).toHaveProperty(
        'value',
        'cenoura',
      )
    })

    expect(wrapper.text()).not.toContain('Usar receita original')
  })
})
