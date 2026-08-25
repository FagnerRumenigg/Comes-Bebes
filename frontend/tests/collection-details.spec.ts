import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeAll, describe, expect, it, vi } from 'vitest'

import type { CollectionResponse, LoginResponse } from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import CollectionDetailsView from '@/views/CollectionDetailsView.vue'
import { useAuthStore } from '@/stores/auth.store'
import { mockServer } from './setup'

beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.open = true
  }
  HTMLDialogElement.prototype.close = function close() {
    this.open = false
    this.dispatchEvent(new Event('close'))
  }
})

const ownerId = 'a1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'

const baseCollection: CollectionResponse = {
  id: 'col-1',
  authorId: ownerId,
  authorUsername: 'maria_cozinha',
  authorDisplayName: 'Maria',
  name: 'Sobremesas de domingo',
  description: null,
  visibility: 'PUBLIC',
  publicationsCount: 0,
  followersCount: 2,
  followedByCurrentUser: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

async function setup(collection: CollectionResponse, session?: LoginResponse) {
  mockServer.use(
    http.get('*/collections/:id', () => HttpResponse.json(collection)),
    http.get('*/collections/:id/publications', () =>
      HttpResponse.json({
        content: [],
        page: 1,
        size: 30,
        totalElements: 0,
        totalPages: 1,
        first: true,
        last: true,
      }),
    ),
  )

  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  if (session) {
    authStore.acceptSession(session)
    setAccessTokenProvider(() => authStore.accessToken)
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/colecoes/:id', component: CollectionDetailsView },
      { path: '/salvos', component: { template: '<div>Salvos</div>' } },
      { path: '/u/:username', component: { template: '<div>Perfil</div>' } },
      { path: '/', component: { template: '<div>Feed</div>' } },
    ],
  })
  await router.push(`/colecoes/${collection.id}`)
  await router.isReady()

  const wrapper = mount(CollectionDetailsView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
  await vi.waitFor(() => expect(wrapper.text()).toContain(collection.name))
  return { wrapper, router }
}

function session(userId: string, username: string): LoginResponse {
  return {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresInSeconds: 3600,
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    userId,
    username,
    role: 'USER',
    onboardingCompleted: true,
    hasUnseenPatchNotes: false,
    emailRequired: false,
    sessionId: 'b1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
    deviceId: 'c1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
  }
}

describe('detalhes da coleção', () => {
  it('dono vê nível de visibilidade e ações de dono; visitante não', async () => {
    const { wrapper } = await setup(baseCollection, session(ownerId, 'maria_cozinha'))

    expect(wrapper.text()).toContain('Pública')
    expect(wrapper.text()).toContain('Compartilhar')
    expect(wrapper.text()).toContain('Quem pode ver')
    expect(wrapper.text()).toContain('Excluir')
  })

  it('visitante não vê o nível de visibilidade nem ações de dono', async () => {
    const { wrapper } = await setup(
      baseCollection,
      session('b1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f99', 'outro_usuario'),
    )

    expect(wrapper.find('.collection-details-view__meta-icon').exists()).toBe(false)
    const buttonLabels = wrapper.findAll('button').map((button) => button.text())
    expect(buttonLabels).not.toContain('Excluir')
    expect(buttonLabels).not.toContain('Quem pode ver')
    expect(buttonLabels).not.toContain('Editar')
  })

  it('avisa sobre perda de seguidores ao rebaixar coleção pública com seguidores', async () => {
    const { wrapper } = await setup(baseCollection, session(ownerId, 'maria_cozinha'))

    const visibilityButton = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Quem pode ver')
    await visibilityButton!.trigger('click')
    await flushPromises()

    const privateOption = wrapper.findAll('[role="radio"]').find((el) => el.text().includes('Só minha'))
    await privateOption!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('pessoas seguem')
    expect(wrapper.text()).toContain('perdem o acesso')
  })

  it('mostra vazio diferente para dono e visitante', async () => {
    const { wrapper: ownerWrapper } = await setup(baseCollection, session(ownerId, 'maria_cozinha'))
    expect(ownerWrapper.text()).toContain('Coleção ainda vazia')
    expect(ownerWrapper.text()).toContain('Ver o que andaram cozinhando')

    const { wrapper: visitorWrapper } = await setup(
      baseCollection,
      session('b1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f99', 'outro_usuario'),
    )
    expect(visitorWrapper.text()).toContain('Nada por aqui ainda')
    expect(visitorWrapper.text()).toContain('ainda não guardou nada')
  })
})
