import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { LoginResponse } from '@/api/generated/models'
import PasswordInput from '@/features/auth/components/PasswordInput.vue'
import { AUTH_STORAGE_KEY, useAuthStore } from '@/stores/auth.store'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import { mockServer } from './setup'

const session: LoginResponse = {
  accessToken: 'access-token',
  refreshToken: 'mock-refresh-fagner-test',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  userId: '71131447-a2a0-4996-a336-a8c3555bb327',
  username: 'fagner',
  role: 'USER',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  emailRequired: false,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  sessionId: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
  deviceId: 'e1f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
}

afterEach(() => {
  window.localStorage.clear()
  window.sessionStorage.clear()
})

describe('store de sessão', () => {
  it('persiste no storage escolhido e mantém o access token somente em memória', () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()

    authStore.acceptSession(session, true)

    const stored = JSON.parse(window.localStorage.getItem(AUTH_STORAGE_KEY) ?? '{}') as Record<
      string,
      unknown
    >
    expect(stored.refreshToken).toBe(session.refreshToken)
    expect(stored.accessToken).toBeUndefined()
    expect(window.sessionStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    expect(authStore.accessToken).toBe('access-token')
    authStore.dispose()
  })

  it('renova uma sessão lembrada durante a inicialização', async () => {
    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        refreshToken: 'mock-refresh-fagner-persisted',
        userId: session.userId,
        username: session.username,
        role: session.role,
        onboardingCompleted: true,
        hasUnseenPatchNotes: false,
        emailRequired: false,
        remember: true,
      }),
    )
    setActivePinia(createPinia())
    const authStore = useAuthStore()

    await authStore.initialize()

    expect(authStore.authenticated).toBe(true)
    expect(authStore.identity?.username).toBe('fagner')
    expect(authStore.accessToken).toMatch(/^mock-access-fagner-/)
    authStore.dispose()
  })

  it('recupera a sessão quando outra aba já rotacionou o refresh token', async () => {
    mockServer.use(
      http.post('*/auth/refresh', async ({ request }) => {
        const body = (await request.json()) as { refreshToken: string }
        if (body.refreshToken === 'stale-token') {
          return HttpResponse.json({ message: 'Token inválido.' }, { status: 401 })
        }
        return HttpResponse.json({
          ...session,
          accessToken: 'renewed-access-token',
          refreshToken: 'renewed-refresh-token',
        })
      }),
    )
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.acceptSession(session, true)
    // Simula outra aba que já rotacionou o refresh token e persistiu o novo
    // valor no localStorage, enquanto esta aba ainda tem o token antigo
    // (agora inválido) em memória.
    authStore.refreshToken = 'stale-token'
    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        refreshToken: 'fresh-token-from-other-tab',
        userId: session.userId,
        username: session.username,
        role: session.role,
        onboardingCompleted: true,
        hasUnseenPatchNotes: false,
        emailRequired: false,
        remember: true,
      }),
    )

    const result = await authStore.renewSession()

    expect(result).toBe(true)
    expect(authStore.authenticated).toBe(true)
    expect(authStore.accessToken).toBe('renewed-access-token')
    authStore.dispose()
  })

  it('desloga quando o refresh token está mesmo inválido em toda parte', async () => {
    mockServer.use(
      http.post('*/auth/refresh', () =>
        HttpResponse.json({ message: 'Token inválido.' }, { status: 401 }),
      ),
    )
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.acceptSession(session, true)

    const result = await authStore.renewSession()

    expect(result).toBe(false)
    expect(authStore.authenticated).toBe(false)
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    authStore.dispose()
  })

  it('revoga e limpa a sessão ao sair', async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.acceptSession(session, false)

    await authStore.logout()

    expect(authStore.authenticated).toBe(false)
    expect(authStore.identity).toBeNull()
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    expect(window.sessionStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })

  it('deduplica renovações simultâneas e persiste o refresh token rotacionado', async () => {
    let refreshRequests = 0
    const rotatedSession = {
      ...session,
      accessToken: 'rotated-access-token',
      refreshToken: 'rotated-refresh-token',
    }
    mockServer.use(
      http.post('*/auth/refresh', async () => {
        refreshRequests += 1
        await delay(20)
        return HttpResponse.json(rotatedSession)
      }),
    )
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.acceptSession(session, true)

    const results = await Promise.all([authStore.renewSession(), authStore.renewSession()])

    expect(results).toEqual([true, true])
    expect(refreshRequests).toBe(1)
    expect(authStore.accessToken).toBe('rotated-access-token')
    expect(authStore.refreshToken).toBe('rotated-refresh-token')
    expect(JSON.parse(window.localStorage.getItem(AUTH_STORAGE_KEY) ?? '{}')).toMatchObject({
      refreshToken: 'rotated-refresh-token',
    })
    authStore.dispose()
  })
})

describe('formulários de autenticação', () => {
  it('permite exibir e ocultar a senha por botão', async () => {
    const wrapper = mount(PasswordInput, {
      props: { label: 'Senha', modelValue: 'segredo123' },
    })

    expect(wrapper.get('input').attributes('type')).toBe('password')
    await wrapper.get('button').trigger('click')
    expect(wrapper.get('input').attributes('type')).toBe('text')
    expect(wrapper.get('button').attributes('aria-label')).toBe('Ocultar senha')
  })

  it('valida login vazio sem enviar a requisição', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: LoginView },
        { path: '/', component: { template: '<p>Feed</p>' } },
      ],
    })
    await router.push('/login')
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    await wrapper.get('form').trigger('submit')

    expect(wrapper.findAll('[role="alert"]').map((item) => item.text())).toEqual([
      'Informe seu e-mail ou usuário.',
      'Informe sua senha.',
    ])
  })

  it('preserva cadastro preenchido quando a confirmação diverge', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/cadastro', component: RegisterView },
        { path: '/login', component: LoginView },
      ],
    })
    await router.push('/cadastro')
    const wrapper = mount(RegisterView, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    await wrapper.get('#register-display-name').setValue('Cozinha de Teste')
    await wrapper.get('#register-email').setValue('cozinha@exemplo.com')
    await wrapper.get('#register-password').setValue('MinhaSenha123!')
    await wrapper.get('#register-confirm-password').setValue('OutraSenha123!')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('#register-email').element).toHaveProperty('value', 'cozinha@exemplo.com')
    expect(wrapper.get('[role="alert"]').text()).toBe('As senhas precisam ser iguais.')
  })

  it('mostra a tela de sucesso ao criar a conta e vai para o feed ao continuar', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/cadastro', component: RegisterView },
        { path: '/login', component: LoginView },
        { path: '/', component: { template: '<p>Feed</p>' } },
      ],
    })
    await router.push('/cadastro')
    const wrapper = mount(RegisterView, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
      },
    })

    await wrapper.get('#register-display-name').setValue('Cozinha de Teste')
    await wrapper.get('#register-email').setValue('cozinha-nova@exemplo.com')
    await wrapper.get('#register-password').setValue('MinhaSenha123!')
    await wrapper.get('#register-confirm-password').setValue('MinhaSenha123!')
    await wrapper.get('form').trigger('submit')
    await vi.waitFor(() => expect(wrapper.text()).toContain('Pronto, Cozinha de Teste!'))

    expect(wrapper.text()).toContain('Pronto, Cozinha de Teste!')
    expect(wrapper.text()).toContain('cozinha-nova@exemplo.com')
    expect(wrapper.find('form').exists()).toBe(false)

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/')
  })
})
