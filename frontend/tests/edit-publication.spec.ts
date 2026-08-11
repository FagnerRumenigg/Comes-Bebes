import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'

import type {
  PublicationResponse,
  RecipeResponse,
  UpdatePublicationRequest,
} from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import EditPublicationView from '@/views/EditPublicationView.vue'
import { useAuthStore } from '@/stores/auth.store'
import { mockPublications } from '@/mocks/fixtures/publications'
import { mockServer } from './setup'

describe('edição de publicação', () => {
  it('carrega a receita do autor e envia o título corrigido com os dados preservados', async () => {
    const basePublication = mockPublications[1]!
    const authorId = '71131447-a2a0-4996-a336-a8c3555bb327'
    const publication: PublicationResponse = { ...basePublication, authorId }
    const recipe = basePublication.recipePreview as RecipeResponse
    let updatedPayload: UpdatePublicationRequest | undefined

    mockServer.use(
      http.get('*/publications/:id', () => HttpResponse.json(publication)),
      http.get('*/publications/:id/recipe', () => HttpResponse.json(recipe)),
      http.patch('*/publications/:id', async ({ request }) => {
        updatedPayload = (await request.json()) as UpdatePublicationRequest
        return HttpResponse.json({ ...publication, ...updatedPayload })
      }),
    )

    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.acceptSession({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      userId: authorId,
      username: 'fagner',
      role: 'USER',
      onboardingCompleted: true,
      sessionId: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
    })
    setAccessTokenProvider(() => authStore.accessToken)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/publicacoes/:id/editar', component: EditPublicationView },
        { path: '/publicacoes/:id', component: { template: '<div>Detalhes</div>' } },
      ],
    })
    await router.push(`/publicacoes/${publication.id}/editar`)
    await router.isReady()
    const wrapper = mount(EditPublicationView, {
      global: {
        plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    await vi.waitFor(() => expect(wrapper.find('form').exists()).toBe(true))
    const titleInput = wrapper
      .findAll('input')
      .find((input) => input.attributes('maxlength') === '150')
    expect(titleInput).toBeDefined()
    await titleInput!.setValue('Bolo de cenoura corrigido')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    await vi.waitFor(() => expect(updatedPayload?.title).toBe('Bolo de cenoura corrigido'))
    expect(updatedPayload?.recipe?.instructions).toBe(recipe.instructions)
    expect(updatedPayload?.recipe?.ingredients).toHaveLength(recipe.ingredients.length)
  })

  it('permite que um admin edite a publicação de outro autor', async () => {
    const basePublication = mockPublications[1]!
    const authorId = '71131447-a2a0-4996-a336-a8c3555bb327'
    const adminId = 'a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'
    const publication: PublicationResponse = { ...basePublication, authorId }
    const recipe = basePublication.recipePreview as RecipeResponse
    let updatedPayload: UpdatePublicationRequest | undefined

    mockServer.use(
      http.get('*/publications/:id', () => HttpResponse.json(publication)),
      http.get('*/publications/:id/recipe', () => HttpResponse.json(recipe)),
      http.patch('*/publications/:id', async ({ request }) => {
        updatedPayload = (await request.json()) as UpdatePublicationRequest
        return HttpResponse.json({ ...publication, ...updatedPayload })
      }),
    )

    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.acceptSession({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      userId: adminId,
      username: 'admin',
      role: 'ADMIN',
      onboardingCompleted: true,
      sessionId: 'b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
    })
    setAccessTokenProvider(() => authStore.accessToken)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/publicacoes/:id/editar', component: EditPublicationView },
        { path: '/publicacoes/:id', component: { template: '<div>Detalhes</div>' } },
      ],
    })
    await router.push(`/publicacoes/${publication.id}/editar`)
    await router.isReady()
    const wrapper = mount(EditPublicationView, {
      global: {
        plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    await vi.waitFor(() => expect(wrapper.find('form').exists()).toBe(true))
    expect(wrapper.text()).not.toContain('Essa publicação não pertence a você.')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    await vi.waitFor(() => expect(updatedPayload).toBeDefined())
  })
})
