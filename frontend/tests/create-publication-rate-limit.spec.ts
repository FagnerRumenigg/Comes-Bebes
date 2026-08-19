import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { setAccessTokenProvider } from '@/api/client'
import CreatePublicationView from '@/views/CreatePublicationView.vue'
import { useAuthStore } from '@/stores/auth.store'
import { mockServer } from './setup'

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
    sessionId: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
    deviceId: 'e1f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
  })
  setAccessTokenProvider(() => authStore.accessToken)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/publicar', component: CreatePublicationView },
      { path: '/publicacoes/:id', component: { template: '<div>Detalhes</div>' } },
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

describe('limite de publicações (429)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('desabilita o botão com contagem regressiva e reabilita após o limite expirar', async () => {
    vi.useFakeTimers()
    // O componente só reavalia o cooldown a cada 1s (setInterval); com
    // nextAvailableAt a +1.2s, o botão só reabilita no tick de +2s, não antes.
    const nextAvailableAt = new Date(Date.now() + 1_200).toISOString()
    mockServer.use(
      http.post('*/publications', () =>
        HttpResponse.json(
          {
            status: 429,
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'Limite de publicações excedido. Tente novamente mais tarde.',
            nextAvailableAt,
          },
          { status: 429 },
        ),
      ),
    )

    const wrapper = await mountCreate()
    await selectImage(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Limite de publicações atingido')
    let submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
    expect(submitButton.text()).toMatch(/Aguarde \d+s/)

    await vi.advanceTimersByTimeAsync(2_000)
    await flushPromises()

    submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeUndefined()
    expect(submitButton.text()).toBe('Publicar')
    expect(wrapper.text()).not.toContain('Limite de publicações atingido')
  })
})
