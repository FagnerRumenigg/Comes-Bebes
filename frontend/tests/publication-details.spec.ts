import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeAll, describe, expect, it, vi } from 'vitest'

import type { LoginResponse, PublicationResponse } from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import PublicationDetailsView from '@/views/PublicationDetailsView.vue'
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

async function setup(publication: PublicationResponse, session?: LoginResponse) {
  mockServer.use(http.get('*/publications/:id', () => HttpResponse.json(publication)))

  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  if (session) {
    authStore.acceptSession(session)
    setAccessTokenProvider(() => authStore.accessToken)
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/publicacoes/:id', component: PublicationDetailsView },
      { path: '/publicacoes/:id/editar', component: { template: '<div>Editar</div>' } },
      { path: '/', component: { template: '<div>Feed</div>' } },
    ],
  })
  await router.push(`/publicacoes/${publication.id}`)
  await router.isReady()

  const wrapper = mount(PublicationDetailsView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
  await vi.waitFor(() => expect(wrapper.text()).toContain(publication.title))
  return { wrapper, router }
}

const adminSession = (adminId: string): LoginResponse => ({
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId: adminId,
  username: 'admin',
  role: 'ADMIN',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  sessionId: 'c2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f33',
  deviceId: 'e2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f33',
})

const strangerSession = (userId: string): LoginResponse => ({
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId,
  username: 'outro_usuario',
  role: 'USER',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  sessionId: 'd2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f44',
  deviceId: 'f2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f44',
})

describe('detalhes da publicação', () => {
  it('mostra editar e excluir para um admin em publicação de outro autor', async () => {
    const publication = mockPublications[1]!
    const adminId = 'a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'

    const { wrapper } = await setup(publication, adminSession(adminId))

    expect(wrapper.text()).toContain('Editar publicação')
    expect(wrapper.text()).toContain('Excluir publicação')
  })

  it('não mostra editar nem excluir para um usuário comum que não é o autor', async () => {
    const publication = mockPublications[1]!
    const strangerId = 'e2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f55'

    const { wrapper } = await setup(publication, strangerSession(strangerId))

    expect(wrapper.text()).not.toContain('Editar publicação')
    expect(wrapper.find('.publication-details__delete').exists()).toBe(false)
  })

  it('exibe a indicação de edição administrativa quando aplicável', async () => {
    const publication: PublicationResponse = { ...mockPublications[0]!, editedByAdmin: true }

    const { wrapper } = await setup(publication)

    expect(wrapper.text()).toContain('Editada por um administrador')
  })

  it('não exibe a indicação de edição administrativa por padrão', async () => {
    const publication = mockPublications[0]!

    const { wrapper } = await setup(publication)

    expect(wrapper.text()).not.toContain('Editada por um administrador')
  })

  it('exclui a publicação ao confirmar e navega para o feed', async () => {
    const publication = mockPublications[1]!
    const adminId = 'a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'
    let deleteCalled = false
    mockServer.use(
      http.delete('*/publications/:id', () => {
        deleteCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const { wrapper, router } = await setup(publication, adminSession(adminId))

    await wrapper.find('.publication-details__delete').trigger('click')
    await flushPromises()
    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Excluir')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')
    await flushPromises()

    await vi.waitFor(() => expect(deleteCalled).toBe(true))
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/'))
  })
})
