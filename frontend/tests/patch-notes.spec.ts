import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { PatchNoteResponse } from '@/api/generated/models'
import { useAuthStore } from '@/stores/auth.store'
import PatchNotesView from '@/views/PatchNotesView.vue'
import { mockServer } from './setup'

const notes: PatchNoteResponse[] = [
  {
    id: 'a4f0f2b0-df43-4b40-9df1-4f6da3e6f36e',
    title: 'Rodapé com versão',
    body: 'Agora o app mostra a versão publicada no rodapé.',
    publishedAt: '2026-08-10T12:00:00Z',
  },
  {
    id: 'b4f0f2b0-df43-4b40-9df1-4f6da3e6f37f',
    title: 'Reações expandidas',
    body: 'Novas reações disponíveis nas publicações.',
    publishedAt: '2026-08-12T09:00:00Z',
  },
]

function mountPatchNotesView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.status = 'authenticated'
  authStore.identity = {
    userId: '71131447-a2a0-4996-a336-a8c3555bb327',
    username: 'fagner',
    role: 'USER',
    onboardingCompleted: true,
    hasUnseenPatchNotes: true,
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'feed', component: { template: '<div>Feed</div>' } },
      { path: '/novidades', name: 'patch-notes', component: PatchNotesView },
    ],
  })

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })

  return { authStore, router, queryClient, pinia }
}

afterEach(() => mockServer.resetHandlers())

describe('notas de versão', () => {
  it('exibe as notas não vistas e confirma leitura', async () => {
    mockServer.use(
      http.get('*/patch-notes/unseen', () => HttpResponse.json(notes)),
      http.patch('*/users/:id/patch-notes/seen', () => new HttpResponse(null, { status: 204 })),
    )

    const { authStore, router, queryClient, pinia } = mountPatchNotesView()
    await router.push('/novidades')
    await router.isReady()

    const wrapper = mount(PatchNotesView, {
      global: { plugins: [pinia, router, [VueQueryPlugin, { queryClient }]] },
    })

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Rodapé com versão')
      expect(wrapper.text()).toContain('Reações expandidas')
    })

    await wrapper.get('button').trigger('click')

    await vi.waitFor(() => {
      expect(authStore.identity?.hasUnseenPatchNotes).toBe(false)
    })
    expect(router.currentRoute.value.name).toBe('feed')
  })

  it('permite tentar novamente após falha ao carregar as novidades', async () => {
    mockServer.use(
      http.get('*/patch-notes/unseen', () =>
        HttpResponse.json({ message: 'Falha temporária.' }, { status: 503 }),
      ),
    )

    const { router, queryClient, pinia } = mountPatchNotesView()
    await router.push('/novidades')
    await router.isReady()

    const wrapper = mount(PatchNotesView, {
      global: { plugins: [pinia, router, [VueQueryPlugin, { queryClient }]] },
    })

    await vi.waitFor(() => {
      expect(wrapper.get('[role="alert"]').text()).toContain('Falha temporária.')
    })

    mockServer.use(http.get('*/patch-notes/unseen', () => HttpResponse.json(notes)))
    await wrapper.get('[role="alert"] button').trigger('click')

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Rodapé com versão')
    })
  })
})
