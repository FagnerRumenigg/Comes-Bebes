import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'

import { setAccessTokenProvider } from '@/api/client'
import CreatePublicationView from '@/views/CreatePublicationView.vue'
import { useAuthStore } from '@/stores/auth.store'

async function mountCreate(): Promise<VueWrapper> {
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
    emailRequired: false,
    sessionId: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
    deviceId: 'e1f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
  })
  setAccessTokenProvider(() => authStore.accessToken)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/publicar', component: CreatePublicationView },
      { path: '/', component: { template: '<div>Feed</div>' } },
    ],
  })
  await router.push('/publicar')
  await router.isReady()

  return mount(CreatePublicationView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
}

async function selectImage(wrapper: VueWrapper): Promise<void> {
  const input = wrapper.get('input[type="file"]')
  const file = new File(['fake'], 'prato.png', { type: 'image/png' })
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
}

describe('publicar — tela de sucesso', () => {
  it('mostra "Publicado!" na mesma tela em vez de navegar direto', async () => {
    const wrapper = await mountCreate()
    await selectImage(wrapper)
    await wrapper.get('form').trigger('submit')
    await vi.waitFor(() => expect(wrapper.get('h1').text()).toBe('Publicado!'))
    expect(wrapper.text()).toContain('Ver no feed')
    expect(wrapper.text()).toContain('Publicar outra coisa')
  })

  it('"Publicar outra coisa" volta ao formulário vazio', async () => {
    const wrapper = await mountCreate()
    await selectImage(wrapper)
    await wrapper.get('form').trigger('submit')
    await vi.waitFor(() => expect(wrapper.get('h1').text()).toBe('Publicado!'))

    const publishAnotherButton = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Publicar outra coisa')
    expect(publishAnotherButton).toBeDefined()
    await publishAnotherButton!.trigger('click')
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('O que você fez?')
    expect(wrapper.find('.create-publication__photo--filled').exists()).toBe(false)
  })
})
