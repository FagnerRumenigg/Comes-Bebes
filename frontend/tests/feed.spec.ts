import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { PageResponsePublicationResponse } from '@/api/generated/models'
import PublicationCard from '@/components/publication/PublicationCard.vue'
import { mockPublications } from '@/mocks/fixtures/publications'
import { useAuthStore } from '@/stores/auth.store'
import FeedView from '@/views/FeedView.vue'
import { mockServer } from './setup'

afterEach(() => vi.unstubAllGlobals())

async function mountFeed(options?: { authenticated?: boolean; initialPath?: string }) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: FeedView },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/u/:username', component: { template: '<div />' } },
      { path: '/publicacoes/:id', component: { template: '<div />' } },
    ],
  })
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  const pinia = createPinia()

  if (options?.authenticated) {
    const authStore = useAuthStore(pinia)
    authStore.status = 'authenticated'
    authStore.identity = {
      userId: 'user-1',
      username: 'visitante',
      role: 'USER',
      onboardingCompleted: true,
      hasUnseenPatchNotes: false,
      emailRequired: false,
    }
  }

  await router.push(options?.initialPath ?? '/')
  await router.isReady()

  const wrapper = mount(FeedView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient }]],
    },
  })

  return { wrapper, router }
}

describe('feed', () => {
  it('exibe carregamento e renderiza apenas publicações públicas para visitantes', async () => {
    const { wrapper } = await mountFeed()

    expect(wrapper.find('[aria-label="Carregando feed"]').exists()).toBe(true)

    await vi.waitFor(() => {
      expect(wrapper.findAllComponents(PublicationCard)).toHaveLength(3)
    })
    expect(wrapper.text()).not.toContain('Somente comunidade')
  })

  it('exibe o estado vazio retornado pelo endpoint', async () => {
    mockServer.use(
      http.get('*/publications/feed', () =>
        HttpResponse.json<PageResponsePublicationResponse>({
          content: [],
          page: 1,
          size: 4,
          totalElements: 0,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
    )

    const { wrapper } = await mountFeed()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('A cozinha está quieta por enquanto.')
    })
  })

  it('permite tentar novamente depois de um erro', async () => {
    mockServer.use(
      http.get('*/publications/feed', () =>
        HttpResponse.json({ message: 'Falha temporária no feed.' }, { status: 503 }),
      ),
    )

    const { wrapper } = await mountFeed()
    await vi.waitFor(() => {
      expect(wrapper.get('[role="alert"]').text()).toContain('Falha temporária no feed.')
    })
    expect(wrapper.get('[role="alert"] button').text()).toContain('Tentar novamente')
  })

  it('carrega a próxima página automaticamente ao alcançar o fim da lista', async () => {
    // FeedView usa dois IntersectionObserver independentes (infinite scroll +
    // rastreamento de visualização de cada card) - distinguimos pelas opções
    // (só o sentinel de infinite scroll usa rootMargin) para não confundir
    // um pelo outro neste teste.
    let intersectionCallback: IntersectionObserverCallback | undefined
    const observe = vi.fn()
    const disconnect = vi.fn()

    vi.stubGlobal(
      'IntersectionObserver',
      class IntersectionObserverMock {
        observe: typeof observe
        unobserve = vi.fn()
        disconnect: typeof disconnect

        constructor(callback: IntersectionObserverCallback, options?: IntersectionObserverInit) {
          // A API real só aceita rootMargin em px/%; reproduzimos essa validação
          // aqui para que um valor inválido (ex.: rem) quebre o teste, como
          // aconteceria em um navegador de verdade.
          if (options?.rootMargin && !/^-?\d+(\.\d+)?(px|%)$/.test(options.rootMargin.trim())) {
            throw new TypeError(
              "Failed to construct 'IntersectionObserver': rootMargin must be specified in pixels or percent.",
            )
          }
          if (options?.rootMargin) {
            intersectionCallback = callback
            this.observe = observe
            this.disconnect = disconnect
          } else {
            this.observe = vi.fn()
            this.disconnect = vi.fn()
          }
        }
      },
    )

    mockServer.use(
      http.get('*/publications/feed', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 1)
        return HttpResponse.json<PageResponsePublicationResponse>({
          content: [mockPublications[page - 1]!],
          page,
          size: 4,
          totalElements: 2,
          totalPages: 2,
          first: page === 1,
          last: page === 2,
        })
      }),
    )

    const { wrapper } = await mountFeed()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Risoto de cogumelos')
    })

    expect(observe).toHaveBeenCalled()
    intersectionCallback?.(
      [{ isIntersecting: true } as IntersectionObserverEntry],
      {} as IntersectionObserver,
    )
    await flushPromises()

    await vi.waitFor(() => {
      expect(wrapper.findAllComponents(PublicationCard)).toHaveLength(2)
    })
    expect(wrapper.text()).toContain('Bolo de cenoura com cobertura de chocolate')
    expect(wrapper.text()).toContain('Você chegou ao fim por hoje.')

    wrapper.unmount()
    expect(disconnect).toHaveBeenCalled()
  })

  it('filtra o feed por tipo ao selecionar Pratos e atualiza a URL', async () => {
    let lastTypes: string[] | null = null
    mockServer.use(
      http.get('*/publications/feed', ({ request }) => {
        const params = new URL(request.url).searchParams
        lastTypes = [...params.entries()]
          .filter(([key]) => key.startsWith('types'))
          .map(([, value]) => value)
        return HttpResponse.json<PageResponsePublicationResponse>({
          content: [],
          page: 1,
          size: 4,
          totalElements: 0,
          totalPages: 1,
          first: true,
          last: true,
        })
      }),
    )

    const { wrapper, router } = await mountFeed()
    await vi.waitFor(() => {
      expect(lastTypes).toEqual([])
    })

    await wrapper.get('.feed-view__filter-btn').trigger('click')
    const pratosChip = wrapper.findAll('.feed-view__chip').find((b) => b.text() === 'Só pratos')
    await pratosChip?.trigger('click')
    await wrapper.findAll('button').find((b) => b.text() === 'Ver resultados')?.trigger('click')
    await flushPromises()

    await vi.waitFor(() => {
      expect(router.currentRoute.value.query.tipo).toBe('pratos')
    })
    await vi.waitFor(() => {
      expect(lastTypes).toEqual(['DISH'])
    })
  })

  it('persiste o filtro escolhido apenas para usuários autenticados', async () => {
    mockServer.use(
      http.get('*/publications/feed', () =>
        HttpResponse.json<PageResponsePublicationResponse>({
          content: [],
          page: 1,
          size: 4,
          totalElements: 0,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
    )
    window.localStorage.removeItem('comes-e-bebes:feed-filter')

    const { wrapper } = await mountFeed()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('A cozinha está quieta por enquanto.')
    })

    const receitasButton = wrapper
      .findAll('.feed-view__filter')
      .find((b) => b.text() === 'Receitas')
    await receitasButton?.trigger('click')
    await flushPromises()

    expect(window.localStorage.getItem('comes-e-bebes:feed-filter')).toBeNull()
    window.localStorage.removeItem('comes-e-bebes:feed-filter')
  })

  it('não mostra o divisor "Você está em dia" quando nada foi visto ainda', async () => {
    const { wrapper } = await mountFeed({ authenticated: true })
    await vi.waitFor(() => {
      expect(wrapper.findAllComponents(PublicationCard)).toHaveLength(3)
    })
    expect(wrapper.find('.feed-divider').exists()).toBe(false)
  })

  it('mostra o divisor "Você está em dia" entre publicações não vistas e já vistas', async () => {
    mockServer.use(
      http.get('*/publications/feed', () =>
        HttpResponse.json<PageResponsePublicationResponse>({
          content: [
            { ...mockPublications[0]!, id: 'a', viewedByCurrentUser: false },
            { ...mockPublications[1]!, id: 'b', viewedByCurrentUser: false },
            { ...mockPublications[2]!, id: 'c', viewedByCurrentUser: true },
          ],
          page: 1,
          size: 4,
          totalElements: 3,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
    )

    const { wrapper } = await mountFeed({ authenticated: true })
    await vi.waitFor(() => {
      expect(wrapper.findAllComponents(PublicationCard)).toHaveLength(3)
    })

    const listChildren = wrapper.get('.feed-view__list').element.children
    expect(listChildren).toHaveLength(4)
    expect(listChildren[2]?.className).toContain('feed-divider')
  })

  it('restaura o filtro salvo ao montar sem tipo na URL para usuário autenticado', async () => {
    mockServer.use(
      http.get('*/publications/feed', () =>
        HttpResponse.json<PageResponsePublicationResponse>({
          content: [],
          page: 1,
          size: 4,
          totalElements: 0,
          totalPages: 1,
          first: true,
          last: true,
        }),
      ),
    )
    window.localStorage.setItem(
      'comes-e-bebes:feed-filter',
      JSON.stringify({ tipo: 'receitas', quem: 'EVERYONE', ordem: 'RECENT' }),
    )

    const { router } = await mountFeed({ authenticated: true })

    await vi.waitFor(() => {
      expect(router.currentRoute.value.query.tipo).toBe('receitas')
    })
    window.localStorage.removeItem('comes-e-bebes:feed-filter')
  })
})

describe('PublicationCard', () => {
  it('oferece edição direta ao autor da publicação', () => {
    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.status = 'authenticated'
    authStore.identity = {
      userId: mockPublications[0]!.authorId,
      username: 'autor',
      role: 'USER',
      onboardingCompleted: true,
      hasUnseenPatchNotes: false,
      emailRequired: false,
    }

    const wrapper = mount(PublicationCard, {
      props: { publication: mockPublications[0]! },
      global: {
        plugins: [pinia, [VueQueryPlugin, { queryClient: new QueryClient() }]],
        stubs: { RouterLink: { props: ['to'], template: '<a :data-to="to"><slot /></a>' } },
      },
    })

    const editLink = wrapper.get('.publication-card__edit')
    expect(editLink.text()).toBe('Editar')
    expect(editLink.attributes('data-to')).toBe(`/publicacoes/${mockPublications[0]!.id}/editar`)
  })

  it('avisa o visitante sem redirecionar ao tentar salvar', async () => {
    const { authNotice, dismissAuthNotice } = await import('@/composables/useAuthNotice')
    dismissAuthNotice()
    const wrapper = mount(PublicationCard, {
      props: { publication: mockPublications[0]! },
      global: {
        plugins: [createPinia(), [VueQueryPlugin, { queryClient: new QueryClient() }]],
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

    await wrapper.get('[aria-label="Salvar publicação; é necessário entrar"]').trigger('click')

    expect(authNotice.visible).toBe(true)
    dismissAuthNotice()
  })

  it('resolve imagens do backend preservando o caminho-base da API', () => {
    const wrapper = mount(PublicationCard, {
      props: {
        publication: { ...mockPublications[0]!, imageUrl: '/images/prato.webp' },
      },
      global: {
        plugins: [createPinia(), [VueQueryPlugin, { queryClient: new QueryClient() }]],
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

    expect(wrapper.get('img').attributes('src')).toBe(
      `${window.location.origin}/comesebebes/images/prato.webp`,
    )
  })

  it('representa uma publicação pendente sem exibir a imagem como ativa', () => {
    const wrapper = mount(PublicationCard, {
      props: {
        publication: { ...mockPublications[0]!, status: 'PENDING_VALIDATION' },
      },
      global: {
        plugins: [createPinia(), [VueQueryPlugin, { queryClient: new QueryClient() }]],
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

    expect(wrapper.get('[role="status"]').text()).toContain('A imagem está sendo validada')
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('trata uma URL de imagem expirada', async () => {
    const wrapper = mount(PublicationCard, {
      props: { publication: mockPublications[0]! },
      global: {
        plugins: [createPinia(), [VueQueryPlugin, { queryClient: new QueryClient() }]],
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

    await wrapper.get('img').trigger('error')
    expect(wrapper.text()).toContain('A imagem não está mais disponível.')
    expect(wrapper.get('button').text()).toBe('Tentar novamente')
  })
})
