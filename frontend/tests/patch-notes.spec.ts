import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'

import type { PatchNoteResponse } from '@/api/generated/models'
import PatchNotesModal from '@/components/patchnotes/PatchNotesModal.vue'
import { useAuthStore } from '@/stores/auth.store'
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

function mountModal(
  identityOverrides: Partial<{ onboardingCompleted: boolean; hasUnseenPatchNotes: boolean }> = {},
) {
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
    ...identityOverrides,
  }

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
  const wrapper = mount(PatchNotesModal, {
    global: { plugins: [pinia, [VueQueryPlugin, { queryClient }]] },
  })

  return { authStore, wrapper }
}

beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.open = true
  }
  HTMLDialogElement.prototype.close = function close() {
    this.open = false
    this.dispatchEvent(new Event('close'))
  }
})

afterEach(() => mockServer.resetHandlers())

describe('modal de notas de versão', () => {
  it('abre com as notas não vistas e confirma leitura ao clicar em Entendi', async () => {
    mockServer.use(
      http.get('*/patch-notes/unseen', () => HttpResponse.json(notes)),
      http.patch('*/users/:id/patch-notes/seen', () => new HttpResponse(null, { status: 204 })),
    )

    const { authStore, wrapper } = mountModal()

    await vi.waitFor(() => {
      expect(wrapper.find('dialog').attributes('open')).toBeDefined()
      expect(wrapper.text()).toContain('Rodapé com versão')
      expect(wrapper.text()).toContain('Reações expandidas')
    })

    const confirmButton = wrapper.findAll('button').find((button) => button.text() === 'Entendi')
    await confirmButton?.trigger('click')

    await vi.waitFor(() => {
      expect(authStore.identity?.hasUnseenPatchNotes).toBe(false)
    })
  })

  it('não abre quando não há notas pendentes', () => {
    const { wrapper } = mountModal({ hasUnseenPatchNotes: false })

    expect(wrapper.find('dialog').attributes('open')).toBeUndefined()
  })

  it('não abre enquanto o onboarding estiver pendente', () => {
    const { wrapper } = mountModal({ onboardingCompleted: false, hasUnseenPatchNotes: true })

    expect(wrapper.find('dialog').attributes('open')).toBeUndefined()
  })

  it('marca como visto ao fechar pelo botão de fechar (backdrop/Esc)', async () => {
    mockServer.use(
      http.get('*/patch-notes/unseen', () => HttpResponse.json(notes)),
      http.patch('*/users/:id/patch-notes/seen', () => new HttpResponse(null, { status: 204 })),
    )

    const { authStore, wrapper } = mountModal()

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Rodapé com versão')
    })

    await wrapper.get('[aria-label="Fechar novidades"]').trigger('click')

    await vi.waitFor(() => {
      expect(authStore.identity?.hasUnseenPatchNotes).toBe(false)
    })
  })

  it('permite tentar novamente após falha ao carregar as novidades', async () => {
    mockServer.use(
      http.get('*/patch-notes/unseen', () =>
        HttpResponse.json({ message: 'Falha temporária.' }, { status: 503 }),
      ),
    )

    const { wrapper } = mountModal()

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
