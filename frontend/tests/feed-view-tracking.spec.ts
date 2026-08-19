import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { setAccessTokenProvider } from '@/api/client'
import type { MarkPublicationsViewedRequest, PublicationResponse } from '@/api/generated/models'
import { useFeedViewTracking } from '@/features/feed/useFeedViewTracking'
import { useAuthStore } from '@/stores/auth.store'
import { mockServer } from './setup'

class IntersectionObserverMock {
  callback: IntersectionObserverCallback
  options?: IntersectionObserverInit
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()

  constructor(callback: IntersectionObserverCallback, options?: IntersectionObserverInit) {
    this.callback = callback
    this.options = options
    instances.push(this)
  }
}

let instances: IntersectionObserverMock[] = []

function stubIntersectionObserver(): void {
  instances = []
  vi.stubGlobal('IntersectionObserver', IntersectionObserverMock)
}

function publication(
  overrides: Partial<PublicationResponse> = {},
): Pick<PublicationResponse, 'id' | 'viewedByCurrentUser'> {
  return { id: 'pub-1', viewedByCurrentUser: false, ...overrides }
}

function mountTrackingHost(
  publicationProp: Pick<PublicationResponse, 'id' | 'viewedByCurrentUser'>,
  pinia: ReturnType<typeof createPinia> = createPinia(),
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

  const TestHost = defineComponent({
    setup() {
      const { observeCard } = useFeedViewTracking()
      return () => h('div', { ref: (el) => observeCard(el as Element | null, publicationProp) })
    },
  })

  const wrapper = mount(TestHost, {
    global: { plugins: [pinia, [VueQueryPlugin, { queryClient }]] },
  })

  return { wrapper, pinia }
}

function authenticate(pinia: ReturnType<typeof createPinia>): void {
  const authStore = useAuthStore(pinia)
  authStore.acceptSession({
    accessToken: 'mock-access-fagner-token',
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
}

beforeEach(() => stubIntersectionObserver())
afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
  setAccessTokenProvider(() => null)
})

describe('useFeedViewTracking', () => {
  it('não observa nada para visitantes não autenticados', () => {
    const { wrapper } = mountTrackingHost(publication())
    expect(instances).toHaveLength(0)
    wrapper.unmount()
  })

  it('não observa publicações já vistas', () => {
    const pinia = createPinia()
    authenticate(pinia)
    const { wrapper } = mountTrackingHost(publication({ viewedByCurrentUser: true }), pinia)
    expect(instances).toHaveLength(0)
    wrapper.unmount()
  })

  it('registra a visualização em lote após ~50% visível por 1s, e para de observar o card', async () => {
    let receivedBody: MarkPublicationsViewedRequest | undefined
    mockServer.use(
      http.post('*/publications/views', async ({ request }) => {
        receivedBody = (await request.json()) as MarkPublicationsViewedRequest
        return new HttpResponse(null, { status: 204 })
      }),
    )

    vi.useFakeTimers()
    const pinia = createPinia()
    authenticate(pinia)
    const { wrapper } = mountTrackingHost(publication({ id: 'pub-1' }), pinia)

    expect(instances).toHaveLength(1)
    const observer = instances[0]!
    expect(observer.options?.threshold).toBe(0.5)
    const target = observer.observe.mock.calls[0]![0] as Element
    expect(target).toBeInstanceOf(Element)

    observer.callback(
      [{ target, isIntersecting: true, intersectionRatio: 0.6 } as IntersectionObserverEntry],
      observer as unknown as IntersectionObserver,
    )

    // Antes de completar 1s contínuo visível, nada deve ser enviado.
    await vi.advanceTimersByTimeAsync(900)
    expect(receivedBody).toBeUndefined()

    await vi.advanceTimersByTimeAsync(200)
    expect(observer.unobserve).toHaveBeenCalledWith(target)

    // O lote só é enviado no próximo flush (não uma requisição por card).
    await vi.advanceTimersByTimeAsync(5_000)
    await flushPromises()

    expect(receivedBody?.publicationIds).toEqual(['pub-1'])
    wrapper.unmount()
  })

  it('cancela o timer se o card sair de vista antes de completar 1s', async () => {
    let requestCount = 0
    mockServer.use(
      http.post('*/publications/views', () => {
        requestCount += 1
        return new HttpResponse(null, { status: 204 })
      }),
    )

    vi.useFakeTimers()
    const pinia = createPinia()
    authenticate(pinia)
    const { wrapper } = mountTrackingHost(publication({ id: 'pub-2' }), pinia)

    const observer = instances[0]!
    const target = observer.observe.mock.calls[0]![0] as Element

    observer.callback(
      [{ target, isIntersecting: true, intersectionRatio: 0.6 } as IntersectionObserverEntry],
      observer as unknown as IntersectionObserver,
    )
    await vi.advanceTimersByTimeAsync(500)
    observer.callback(
      [{ target, isIntersecting: false, intersectionRatio: 0 } as IntersectionObserverEntry],
      observer as unknown as IntersectionObserver,
    )
    await vi.advanceTimersByTimeAsync(10_000)
    await flushPromises()

    expect(requestCount).toBe(0)
    wrapper.unmount()
  })

  it('envia visualizações pendentes imediatamente ao desmontar', async () => {
    let receivedBody: MarkPublicationsViewedRequest | undefined
    mockServer.use(
      http.post('*/publications/views', async ({ request }) => {
        receivedBody = (await request.json()) as MarkPublicationsViewedRequest
        return new HttpResponse(null, { status: 204 })
      }),
    )

    vi.useFakeTimers()
    const pinia = createPinia()
    authenticate(pinia)
    const { wrapper } = mountTrackingHost(publication({ id: 'pub-3' }), pinia)

    const observer = instances[0]!
    const target = observer.observe.mock.calls[0]![0] as Element
    observer.callback(
      [{ target, isIntersecting: true, intersectionRatio: 1 } as IntersectionObserverEntry],
      observer as unknown as IntersectionObserver,
    )
    await vi.advanceTimersByTimeAsync(1_000)

    wrapper.unmount()
    await flushPromises()

    expect(receivedBody?.publicationIds).toEqual(['pub-3'])
  })
})
