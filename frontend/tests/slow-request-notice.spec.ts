import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { delay, http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/api/client'
import App from '@/App.vue'
import { backendStatus } from '@/composables/useBackendStatus'
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

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  backendStatus.slowRequest = false
})

function mountApp() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div>Feed</div>' } }],
  })
  return mount(App, {
    global: {
      plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
}

// SlowRequestNotice não é mais renderizado em App.vue (desativado por
// enquanto — a tela de carregando nova, com a animação do bule
// (BackendOfflineScreen.vue), cobre o caso de servidor fora do ar; um
// popup à parte pra qualquer requisição lenta ficou redundante).
// Componente mantido no código pra religar depois se precisar.
describe.skip('aviso de requisição lenta', () => {
  it('mostra o modal só depois do limiar, some quando a requisição termina', async () => {
    mockServer.use(
      http.get('*/ping-slow', async () => {
        await delay(5_000)
        return HttpResponse.json({})
      }),
    )

    const wrapper = mountApp()
    const request = httpClient.get('/ping-slow')

    await vi.advanceTimersByTimeAsync(2_000)
    expect(backendStatus.slowRequest).toBe(false)
    expect(wrapper.find('dialog').element?.open).toBeFalsy()

    await vi.advanceTimersByTimeAsync(1_500)
    await wrapper.vm.$nextTick()
    expect(backendStatus.slowRequest).toBe(true)
    expect(wrapper.text()).toContain('Isso está demorando mais que o normal')

    await vi.advanceTimersByTimeAsync(2_000)
    await request
    await wrapper.vm.$nextTick()

    expect(backendStatus.slowRequest).toBe(false)
  })

  it('não mostra o modal quando a requisição termina antes do limiar', async () => {
    mockServer.use(http.get('*/ping-fast', () => HttpResponse.json({})))

    const wrapper = mountApp()
    await httpClient.get('/ping-fast')
    await wrapper.vm.$nextTick()

    expect(backendStatus.slowRequest).toBe(false)
  })
})
