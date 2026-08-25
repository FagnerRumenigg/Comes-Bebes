import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeAll, describe, expect, it, vi } from 'vitest'

import type { CollectionResponse, LoginResponse } from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import SavedView from '@/views/SavedView.vue'
import { useAuthStore } from '@/stores/auth.store'
import { mockPublications } from '@/mocks/fixtures/publications'
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

const userId = 'a1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'

const session: LoginResponse = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId,
  username: 'fagner',
  role: 'USER',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  emailRequired: false,
  sessionId: 'b1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
  deviceId: 'c1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
}

function pageResponse<T>(content: T[]) {
  return {
    content,
    page: 1,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
  }
}

const ownCollection: CollectionResponse = {
  id: 'own-1',
  authorId: userId,
  authorUsername: 'fagner',
  authorDisplayName: 'Fagner',
  name: 'Sobremesas',
  description: null,
  visibility: 'PRIVATE',
  publicationsCount: 3,
  followersCount: 0,
  followedByCurrentUser: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

async function setup(options: { collections?: CollectionResponse[]; saved?: string[] } = {}) {
  const collections = options.collections ?? []
  const savedIds = options.saved ?? []

  mockServer.use(
    http.get('*/users/:id/collections', () => HttpResponse.json(pageResponse(collections))),
    http.get('*/collections/followed', () => HttpResponse.json(pageResponse([]))),
    http.get('*/publications/saved', () =>
      HttpResponse.json(
        pageResponse(savedIds.map((publicationId) => ({ publicationId, savedAt: new Date().toISOString() }))),
      ),
    ),
    http.get('*/publications/:id', ({ params }) => {
      const publication = mockPublications.find((item) => item.id === params.id) ?? mockPublications[0]!
      return HttpResponse.json(publication)
    }),
  )

  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  authStore.acceptSession(session)
  setAccessTokenProvider(() => authStore.accessToken)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/salvos', component: SavedView },
      { path: '/', component: { template: '<div>Feed</div>' } },
    ],
  })
  await router.push('/salvos')
  await router.isReady()

  const wrapper = mount(SavedView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
  await vi.waitFor(() => expect(wrapper.text()).toContain('Salvos'))
  return { wrapper }
}

describe('salvos', () => {
  it('mostra o vazio ilustrado quando não há nada guardado', async () => {
    const { wrapper } = await setup()

    await vi.waitFor(() => expect(wrapper.text()).toContain('Nada guardado ainda'))
    expect(wrapper.text()).toContain('Ver o que andaram cozinhando')
  })

  it('mostra as 3 seções quando há coleções próprias e publicações salvas', async () => {
    const { wrapper } = await setup({
      collections: [ownCollection],
      saved: [mockPublications[0]!.id],
    })

    await vi.waitFor(() => expect(wrapper.text()).toContain('Suas coleções'))
    expect(wrapper.text()).toContain('Tudo que você guardou')
    expect(wrapper.text()).toContain(ownCollection.name)
  })

  it('mostra o nudge de criar coleção quando só há salvos soltos', async () => {
    const { wrapper } = await setup({ saved: [mockPublications[0]!.id] })

    await vi.waitFor(() => expect(wrapper.text()).toContain('Quer separar por tipo?'))
  })
})
