import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import LoginView from '@/views/LoginView.vue'
import { mockServer } from './setup'

const BIOMETRIC_PROMPT_DISMISSED_KEY = 'comes-e-bebes:hide-biometric-enrollment-prompt'

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
  window.localStorage.clear()
  // isWebAuthnSupported() checa `typeof window.PublicKeyCredential === 'function'`
  // - precisa ser uma function de verdade, não um objeto plano.
  const publicKeyCredentialStub = vi.fn() as unknown as typeof PublicKeyCredential
  publicKeyCredentialStub.isUserVerifyingPlatformAuthenticatorAvailable = vi
    .fn()
    .mockResolvedValue(true)
  vi.stubGlobal('PublicKeyCredential', publicKeyCredentialStub)
})

afterEach(() => {
  window.localStorage.clear()
  vi.unstubAllGlobals()
})

async function mountLogin() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/', component: { template: '<p>Feed</p>' } },
      { path: '/onboarding', component: { template: '<p>Onboarding</p>' } },
    ],
  })
  await router.push('/login')
  const wrapper = mount(LoginView, {
    global: {
      plugins: [createPinia(), router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
  return { wrapper, router }
}

async function submitLogin(wrapper: Awaited<ReturnType<typeof mountLogin>>['wrapper']): Promise<void> {
  await wrapper.get('#login-username').setValue('fagner')
  await wrapper.get('#login-password').setValue('SenhaForte123!')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('prompt de ativação de biometria pós-login', () => {
  it('oferece Sim / Não / Não mostrar mais quando elegível', async () => {
    mockServer.use(
      http.post('*/auth/login', () =>
        HttpResponse.json({
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
        }),
      ),
      http.get('*/auth/biometric/status', () => HttpResponse.json({ hasBiometric: false })),
    )

    const { wrapper } = await mountLogin()
    await submitLogin(wrapper)

    expect(wrapper.get('dialog').element.open).toBe(true)
    expect(wrapper.text()).toContain('Ativar biometria?')
    const buttons = wrapper.findAll('button').map((button) => button.text())
    expect(buttons).toContain('Sim')
    expect(buttons).toContain('Não')
    expect(buttons).toContain('Não mostrar mais ao login')
  })

  it('persiste a escolha de não mostrar mais e fecha o prompt', async () => {
    mockServer.use(
      http.post('*/auth/login', () =>
        HttpResponse.json({
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
        }),
      ),
      http.get('*/auth/biometric/status', () => HttpResponse.json({ hasBiometric: false })),
    )

    const { wrapper } = await mountLogin()
    await submitLogin(wrapper)

    const dontShowAgain = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Não mostrar mais ao login')
    expect(dontShowAgain).toBeDefined()
    await dontShowAgain!.trigger('click')

    expect(window.localStorage.getItem(BIOMETRIC_PROMPT_DISMISSED_KEY)).toBe('true')
  })

  it('não oferece o prompt de novo quando já foi permanentemente dispensado', async () => {
    window.localStorage.setItem(BIOMETRIC_PROMPT_DISMISSED_KEY, 'true')
    mockServer.use(
      http.post('*/auth/login', () =>
        HttpResponse.json({
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
        }),
      ),
    )

    const { wrapper } = await mountLogin()
    await submitLogin(wrapper)

    // O <dialog> sempre renderiza o conteúdo no DOM - o que importa é não
    // ter sido aberto via showModal() (atributo `open`).
    const dialog = wrapper.find('dialog')
    expect(dialog.exists() && dialog.element.open).toBeFalsy()
  })
})
