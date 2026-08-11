import { createPinia } from 'pinia'
import { flushPromises, mount, shallowMount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, type RouteLocationNormalized } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import AdminLayout from '@/app/layouts/AdminLayout.vue'
import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import { resolveRouteAccess } from '@/app/router/guards'
import { navigationState, routes } from '@/app/router'
import App from '@/App.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import MobileNavigation from '@/components/layout/MobileNavigation.vue'
import ThemeSwitch from '@/components/layout/ThemeSwitch.vue'
import { authNotice, dismissAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes,
  })
}

afterEach(() => {
  vi.useRealTimers()
  window.localStorage.clear()
  delete document.documentElement.dataset.theme
  navigationState.pending = false
  navigationState.error = null
  dismissAuthNotice()
})

describe('mapa de rotas', () => {
  it('resolve todas as rotas documentadas no layout correto', () => {
    const router = createTestRouter()
    const expectedRoutes = [
      ['/', 'feed', 'public'],
      ['/buscar', 'search', 'public'],
      ['/publicacoes/42', 'publication-details', 'public'],
      ['/publicacoes/42/editar', 'edit-publication', 'authenticated'],
      ['/u/cozinha', 'profile', 'public'],
      ['/publicar', 'create-publication', 'authenticated'],
      ['/publicar/minha-versao/42', 'create-my-version', 'authenticated'],
      ['/salvos', 'saved', 'authenticated'],
      ['/notificacoes', 'notifications', 'authenticated'],
      ['/cadastro', 'register', 'guest'],
      ['/login', 'login', 'guest'],
      ['/admin/moderacao', 'moderation-queue', 'admin'],
      ['/admin/moderacao/42', 'moderation-case', 'admin'],
      ['/rota-inexistente', 'not-found', 'public'],
    ]

    for (const [path, name, access] of expectedRoutes) {
      const resolved = router.resolve(path)
      expect(resolved.name, path).toBe(name)
      expect(resolved.meta.access, path).toBe(access)
    }

    expect(router.resolve('/').matched[0]?.components?.default).toBe(AppLayout)
    expect(router.resolve('/login').matched[0]?.components?.default).toBe(AuthLayout)
    expect(router.resolve('/admin/moderacao').matched[0]?.components?.default).toBe(AdminLayout)
  })

  it('protege visitante, usuário e administrador', () => {
    const router = createTestRouter()
    const login = router.resolve('/login') as RouteLocationNormalized
    const publish = router.resolve('/publicar') as RouteLocationNormalized
    const moderation = router.resolve('/admin/moderacao') as RouteLocationNormalized

    expect(resolveRouteAccess(login, { authenticated: true, role: 'USER' })).toEqual({
      name: 'feed',
    })
    expect(resolveRouteAccess(publish, { authenticated: false, role: null })).toEqual({
      name: 'login',
      query: { redirect: '/publicar' },
    })
    expect(resolveRouteAccess(moderation, { authenticated: true, role: 'USER' })).toEqual({
      name: 'feed',
    })
    expect(resolveRouteAccess(moderation, { authenticated: true, role: 'ADMIN' })).toBe(true)
  })
})

describe('layouts e navegação', () => {
  it('mantém os landmarks próprios de cada layout', () => {
    const global = {
      plugins: [createPinia()],
      stubs: ['RouterLink', 'RouterView', 'PageContainer', 'ThemeSwitch'],
    }

    const appLayout = shallowMount(AppLayout, { global })
    const authLayout = shallowMount(AuthLayout, { global })
    const adminLayout = mount(AdminLayout, {
      global: {
        plugins: [createPinia()],
        stubs: ['RouterLink', 'RouterView', 'ThemeSwitch'],
      },
    })

    expect(appLayout.find('main').exists()).toBe(true)
    expect(authLayout.find('aside').attributes('aria-label')).toBe('Apresentação do Comes&Bebes')
    expect(adminLayout.find('nav').attributes('aria-label')).toBe('Navegação administrativa')
  })

  it('oferece os destinos principais no desktop e mobile', async () => {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const global = { plugins: [createPinia(), router] }
    const header = mount(AppHeader, { global })
    const mobile = mount(MobileNavigation, { global })

    expect(header.get('nav').attributes('aria-label')).toBe('Navegação principal')
    expect(
      header
        .get('nav')
        .findAll('a, button')
        .map((item) => item.text()),
    ).toEqual(['Buscar', 'Publicar', 'Salvos'])
    expect(mobile.findAll('a, button').map((item) => item.text())).toEqual([
      'Início',
      'Buscar',
      'Publicar',
      'Salvos',
      'Entrar',
    ])

    await header.get('nav button').trigger('click')
    expect(authNotice.visible).toBe(true)
  })

  it('oferece a ação de sair na navegação mobile quando autenticado', async () => {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()

    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.status = 'authenticated'
    authStore.identity = {
      userId: 'user-1',
      username: 'cozinha',
      role: 'USER',
      onboardingCompleted: true,
    }

    const mobile = mount(MobileNavigation, { global: { plugins: [pinia, router] } })

    expect(mobile.findAll('a, button').map((item) => item.text())).toEqual([
      'Início',
      'Buscar',
      'Publicar',
      'Salvos',
      'Perfil',
      'Sair',
    ])

    await mobile.get('.mobile-navigation__logout').trigger('click')
    await flushPromises()

    expect(authStore.authenticated).toBe(false)
  })

  it('remove automaticamente o aviso de autenticação depois de dois segundos', async () => {
    vi.useFakeTimers()
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()
    const header = mount(AppHeader, { global: { plugins: [createPinia(), router] } })

    await header.get('nav button').trigger('click')
    expect(authNotice.visible).toBe(true)

    vi.advanceTimersByTime(2_000)
    expect(authNotice.visible).toBe(false)
  })

  it('alterna e persiste o tema pelo controle do cabeçalho', async () => {
    const pinia = createPinia()
    const wrapper = mount(ThemeSwitch, { global: { plugins: [pinia] } })
    const button = wrapper.get('button')

    expect(button.attributes('aria-label')).toBe('Ativar tema escuro')
    await button.trigger('click')

    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(window.localStorage.getItem('comes-e-bebes:theme')).toBe('dark')
    expect(button.attributes('aria-label')).toBe('Ativar tema claro')
  })

  it('expõe carregamento, erro global e atalho para o conteúdo', async () => {
    navigationState.pending = true
    navigationState.error = 'Falha ao carregar.'
    const wrapper = mount(App, {
      global: { stubs: ['RouterView'] },
    })

    expect(wrapper.get('.skip-link').attributes('href')).toBe('#main-content')
    expect(wrapper.get('[role="progressbar"]').attributes('aria-label')).toBe('Carregando página')
    expect(wrapper.get('[role="alert"]').text()).toContain('Falha ao carregar.')

    await wrapper.get('[role="alert"] button').trigger('click')
    expect(navigationState.error).toBeNull()
  })
})
