import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { beforeAll, describe, expect, it } from 'vitest'

import type { LoginResponse } from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import SaveButton from '@/components/publication/SaveButton.vue'
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

const session: LoginResponse = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId: 'a1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11',
  username: 'fagner',
  role: 'USER',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  emailRequired: false,
  sessionId: 'b1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
  deviceId: 'c1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22',
}

function mountSaveButton(saved: boolean) {
  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  authStore.acceptSession(session)
  setAccessTokenProvider(() => authStore.accessToken)

  return mount(SaveButton, {
    props: { publicationId: 'pub-1', saved },
    global: {
      plugins: [pinia, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
}

describe('salvar publicação (fluxo fundido)', () => {
  it('salva em 1 toque e mostra o toast com atalho para organizar', async () => {
    mockServer.use(http.put('*/publications/:id/saved', () => new HttpResponse(null, { status: 204 })))

    const wrapper = mountSaveButton(false)
    await wrapper.get('.save-button__control').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Guardado')
    expect(document.body.textContent).toContain('Guardado nos seus salvos.')
    expect(document.body.textContent).toContain('Escolher coleção')
  })

  it('ao tocar de novo (já salvo), abre a folha de organizar em vez de desfazer direto', async () => {
    const wrapper = mountSaveButton(true)
    await wrapper.get('.save-button__control').trigger('click')
    await flushPromises()

    expect(wrapper.get('dialog').attributes('open')).toBeDefined()
    expect(wrapper.text()).toContain('Guardar em')
  })

  it('"Tirar dos salvos" remove e fecha a folha', async () => {
    mockServer.use(
      http.delete('*/publications/:id/saved', () => new HttpResponse(null, { status: 204 })),
      http.get('*/users/:id/collections', () =>
        HttpResponse.json({
          content: [],
          page: 1,
          size: 50,
          totalElements: 0,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
    )

    const wrapper = mountSaveButton(true)
    await wrapper.get('.save-button__control').trigger('click')
    await flushPromises()

    const removeButton = wrapper.findAll('button').find((button) => button.text() === 'Tirar dos salvos')
    expect(removeButton).toBeDefined()
    await removeButton!.trigger('click')
    await flushPromises()

    expect(wrapper.get('dialog').attributes('open')).toBeUndefined()
    expect(wrapper.text()).toContain('Salvar')
    expect(wrapper.text()).not.toContain('Guardado')
  })
})
