import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeAll, describe, expect, it, vi } from 'vitest'

import type { LoginResponse, UserResponse } from '@/api/generated/models'
import { setAccessTokenProvider } from '@/api/client'
import ProfileView from '@/views/ProfileView.vue'
import { useAuthStore } from '@/stores/auth.store'
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

const targetProfile: UserResponse = {
  id: '9c1a1a1a-1111-4a4a-9a9a-1a1a1a1a1a1a',
  username: 'cozinha_alvo',
  displayName: 'Cozinha Alvo',
  role: 'USER',
  status: 'ACTIVE',
  onboardingCompleted: true,
  followedByCurrentUser: false,
}

async function setup(profile: UserResponse, session?: LoginResponse) {
  mockServer.use(
    http.get('*/u/:username', () => HttpResponse.json(profile)),
    http.get('*/users/:id/publications', () =>
      HttpResponse.json({
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 1,
        first: true,
        last: true,
      }),
    ),
  )

  const pinia = createPinia()
  const authStore = useAuthStore(pinia)
  if (session) {
    authStore.acceptSession(session)
    setAccessTokenProvider(() => authStore.accessToken)
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/u/:username', component: ProfileView }],
  })
  await router.push(`/u/${profile.username}`)
  await router.isReady()

  const wrapper = mount(ProfileView, {
    global: {
      plugins: [pinia, router, [VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
  await vi.waitFor(() => expect(wrapper.text()).toContain(profile.displayName))
  return { wrapper }
}

const adminSession = (adminId: string): LoginResponse => ({
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId: adminId,
  username: 'admin',
  role: 'ADMIN',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  emailRequired: false,
  sessionId: 'f2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f66',
  deviceId: 'd2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f66',
})

const userSession = (userId: string): LoginResponse => ({
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  userId,
  username: 'visitante',
  role: 'USER',
  onboardingCompleted: true,
  hasUnseenPatchNotes: false,
  emailRequired: false,
  sessionId: 'a1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f77',
  deviceId: 'd1c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f77',
})

describe('perfil público', () => {
  it('mostra "Bloquear usuário" para admin no perfil de outro usuário', async () => {
    const { wrapper } = await setup(targetProfile, adminSession('a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'))

    expect(wrapper.find('.profile-view__block').exists()).toBe(true)
  })

  it('não mostra "Bloquear usuário" para o próprio admin', async () => {
    const { wrapper } = await setup({ ...targetProfile, id: 'a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11' }, adminSession('a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'))

    expect(wrapper.find('.profile-view__block').exists()).toBe(false)
  })

  it('não mostra "Bloquear usuário" para um usuário comum', async () => {
    const { wrapper } = await setup(targetProfile, userSession('b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'))

    expect(wrapper.find('.profile-view__block').exists()).toBe(false)
  })

  it('não mostra "Bloquear usuário" quando a conta já está bloqueada', async () => {
    const { wrapper } = await setup(
      { ...targetProfile, status: 'BLOCKED' },
      adminSession('a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'),
    )

    expect(wrapper.find('.profile-view__block').exists()).toBe(false)
  })

  it('envia o motivo e bloqueia o usuário', async () => {
    let blockPayload: { reason?: string } | undefined
    mockServer.use(
      http.patch('*/users/:id/block', async ({ request }) => {
        blockPayload = (await request.json()) as { reason?: string }
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const { wrapper } = await setup(targetProfile, adminSession('a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'))

    await wrapper.find('.profile-view__block').trigger('click')
    await flushPromises()
    await wrapper.get('textarea').setValue('Conteúdo repetidamente denunciado.')
    const confirmButton = wrapper.findAll('button').find((button) => button.text() === 'Bloquear')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')
    await flushPromises()

    await vi.waitFor(() => expect(blockPayload?.reason).toBe('Conteúdo repetidamente denunciado.'))
  })

  it('exige um motivo antes de confirmar o bloqueio', async () => {
    const { wrapper } = await setup(targetProfile, adminSession('a2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f11'))

    await wrapper.find('.profile-view__block').trigger('click')
    await flushPromises()
    const confirmButton = wrapper.findAll('button').find((button) => button.text() === 'Bloquear')
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Informe o motivo do bloqueio.')
  })
})

describe('seguir usuário', () => {
  it('não mostra contador de seguidores nem de seguindo', async () => {
    // Nenhum contador público em lugar nenhum do produto (produto5.md v5 §3.1).
    const { wrapper } = await setup(
      targetProfile,
      userSession('b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'),
    )

    expect(wrapper.find('.profile-view__counts').exists()).toBe(false)
  })

  it('mostra "Seguir" no perfil de outro usuário quando ainda não segue', async () => {
    const { wrapper } = await setup(
      { ...targetProfile, followedByCurrentUser: false },
      userSession('b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'),
    )

    expect(wrapper.find('.profile-view__follow').text()).toBe('Seguir')
  })

  it('mostra "Deixar de seguir" quando já segue', async () => {
    const { wrapper } = await setup(
      { ...targetProfile, followedByCurrentUser: true },
      userSession('b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'),
    )

    expect(wrapper.find('.profile-view__follow').text()).toBe('Deixar de seguir')
  })

  it('não mostra o botão de seguir no próprio perfil', async () => {
    const ownId = 'b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'
    const { wrapper } = await setup({ ...targetProfile, id: ownId }, userSession(ownId))

    expect(wrapper.find('.profile-view__follow').exists()).toBe(false)
  })

  it('segue o usuário e atualiza para "Deixar de seguir"', async () => {
    const { wrapper } = await setup(
      { ...targetProfile, followedByCurrentUser: false },
      userSession('b2c6d9d0-4d3a-4a63-9f0e-9a3f5f0a1f22'),
    )

    // A partir daqui sobrescreve o mock de setup(): o refetch disparado pelo clique
    // precisa refletir o novo estado (setup() sempre devolveria followedByCurrentUser: false).
    let following = false
    mockServer.use(
      http.get('*/u/:username', () =>
        HttpResponse.json({ ...targetProfile, followedByCurrentUser: following }),
      ),
      http.put('*/users/:id/follow', () => {
        following = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await wrapper.find('.profile-view__follow button').trigger('click')
    await flushPromises()

    expect(following).toBe(true)
    await vi.waitFor(() => expect(wrapper.find('.profile-view__follow').text()).toBe('Deixar de seguir'))
  })
})
