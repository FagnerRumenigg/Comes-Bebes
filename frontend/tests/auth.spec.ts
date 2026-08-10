import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'

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
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
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
      'Informe seu nome de usuário.',
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
    await wrapper.get('#register-username').setValue('cozinha_teste')
    await wrapper.get('#register-password').setValue('MinhaSenha123!')
    await wrapper.get('#register-confirm-password').setValue('OutraSenha123!')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('#register-username').element).toHaveProperty('value', 'cozinha_teste')
    expect(wrapper.get('[role="alert"]').text()).toBe('As senhas precisam ser iguais.')
  })
})
