import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'

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

afterEach(() => {
  vi.useRealTimers()
})

describe('detecção de backend indisponível', () => {
  it('marca offline quando a requisição não recebe resposta (container desligado)', async () => {
    mockServer.use(http.get('*/ping', () => HttpResponse.error()))

    await expect(httpClient.get('/ping')).rejects.toBeTruthy()
    expect(backendStatus.offline).toBe(true)
  })

  it('marca offline em 502/503/504 (ex.: gateway respondendo no lugar do backend)', async () => {
    mockServer.use(http.get('*/ping', () => HttpResponse.json({}, { status: 503 })))

    await expect(httpClient.get('/ping')).rejects.toBeTruthy()
    expect(backendStatus.offline).toBe(true)
  })

  it('não marca offline para um 4xx normal, e volta a ficar online após uma resposta', async () => {
    mockServer.use(http.get('*/ping', () => HttpResponse.error()))
    await expect(httpClient.get('/ping')).rejects.toBeTruthy()
    expect(backendStatus.offline).toBe(true)

    mockServer.use(http.get('*/ping', () => HttpResponse.json({}, { status: 404 })))
    await expect(httpClient.get('/ping')).rejects.toBeTruthy()
    expect(backendStatus.offline).toBe(false)
  })

  it('não marca offline quando a requisição é cancelada (ex.: navegação desmonta a query)', async () => {
    mockServer.use(
      http.get('*/ping', async () => {
        await new Promise((resolve) => setTimeout(resolve, 200))
        return HttpResponse.json({})
      }),
    )

    const controller = new AbortController()
    const request = httpClient.get('/ping', { signal: controller.signal })
    controller.abort()

    await expect(request).rejects.toBeTruthy()
    expect(backendStatus.offline).toBe(false)
  })

  it('App.vue mostra a tela de fallback e some com o RouterView quando offline', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Feed</div>' } }],
    })
    await router.push('/')
    await router.isReady()

    mockServer.use(http.get('*/actuator/health/liveness', () => HttpResponse.error()))

    backendStatus.offline = true
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    // Estágio inicial (0-10s): só a mensagem, sem botão de retry ainda —
    // é intencional (docs/telas/03-carregando.html), espera curta não
    // merece oferecer saída.
    expect(wrapper.text()).toContain('Preparando tudo para você')
    expect(wrapper.text()).not.toContain('Feed')
    expect(wrapper.find('button').exists()).toBe(false)

    wrapper.unmount()
  })

  it('a tela de fallback some sozinha quando o health check volta a responder', async () => {
    vi.useFakeTimers()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Feed</div>' } }],
    })
    await router.push('/')
    await router.isReady()

    mockServer.use(
      http.get('*/actuator/health/liveness', () => HttpResponse.json({ status: 'UP' })),
    )

    backendStatus.offline = true
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    // Sobe o polling automático (a cada 5s) até ele bater no health check
    // já respondendo — some sozinha, sem precisar de clique manual.
    await vi.advanceTimersByTimeAsync(5_000)

    expect(backendStatus.offline).toBe(false)
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).not.toContain('Preparando tudo para você')
    expect(wrapper.text()).toContain('Feed')

    wrapper.unmount()
  })
})
