import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { beforeAll, describe, expect, it, vi } from 'vitest'

import type { PublicationResponse } from '@/api/generated/models'
import ReactionBar from '@/components/publication/ReactionBar.vue'
import { authNotice, dismissAuthNotice } from '@/composables/useAuthNotice'
import { mockPublications } from '@/mocks/fixtures/publications'
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

const viewerId = '71131447-a2a0-4996-a336-a8c3555bb327'

function mountReactionBar(publication: Partial<PublicationResponse>, authenticated = true) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (authenticated) {
    const authStore = useAuthStore()
    authStore.status = 'authenticated'
    authStore.identity = {
      userId: viewerId,
      username: 'fagner',
      role: 'USER',
      onboardingCompleted: true,
      hasUnseenPatchNotes: false,
    }
  }

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
  return mount(ReactionBar, {
    props: { publication: { ...mockPublications[0]!, ...publication } },
    global: { plugins: [pinia, [VueQueryPlugin, { queryClient }]] },
  })
}

describe('ReactionBar', () => {
  it('mostra as reações principais e o botão + Reagir', () => {
    const wrapper = mountReactionBar({ authorId: 'outro-usuario', selectedReactions: [] })

    expect(wrapper.text()).toContain('Eu comeria')
    expect(wrapper.text()).toContain('Quero fazer')
    expect(wrapper.text()).toContain('Comida afetiva')
    expect(wrapper.text()).toContain('+ Reagir')
  })

  it('alterna uma reação principal ao clicar', async () => {
    mockServer.use(http.put('*/publications/:id/reactions', () => new HttpResponse(null, { status: 204 })))
    const wrapper = mountReactionBar({ authorId: 'outro-usuario', selectedReactions: [] })

    const button = wrapper.findAll('button').find((b) => b.text().includes('Eu comeria'))!
    await button.trigger('click')

    expect(button.attributes('aria-pressed')).toBe('true')
  })

  it('impede uma quarta reação e mostra o aviso de limite', async () => {
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      selectedReactions: ['WOULD_EAT', 'WANT_TO_MAKE', 'COMFORT_FOOD'],
    })

    expect(wrapper.text()).toContain('Você pode escolher até 3 reações para esta publicação.')

    await wrapper.get('button.reaction-bar__more').trigger('click')
    const secondary = wrapper.findAll('button').find((b) => b.text().includes('Preciso da receita!'))!
    expect(secondary.attributes('disabled')).toBeDefined()
  })

  it('abre "+ Reagir" e a reação secundária escolhida passa a aparecer na barra principal', async () => {
    mockServer.use(http.put('*/publications/:id/reactions', () => new HttpResponse(null, { status: 204 })))
    const wrapper = mountReactionBar({ authorId: 'outro-usuario', selectedReactions: [] })

    await wrapper.get('button.reaction-bar__more').trigger('click')
    const recipeButton = wrapper.findAll('button').find((b) => b.text().includes('Preciso da receita!'))!
    await recipeButton.trigger('click')

    await vi.waitFor(() => {
      const barButtons = wrapper.findAll('button').filter((b) => b.text().includes('Preciso da receita!'))
      expect(barButtons.length).toBeGreaterThanOrEqual(1)
      expect(barButtons.some((b) => b.attributes('aria-pressed') === 'true')).toBe(true)
    })
  })

  it('visitante aciona aviso de autenticação em vez de reagir', async () => {
    dismissAuthNotice()
    const wrapper = mountReactionBar({ authorId: 'outro-usuario', selectedReactions: [] }, false)

    const button = wrapper.findAll('button').find((b) => b.text().includes('Eu comeria'))!
    await button.trigger('click')

    expect(authNotice.visible).toBe(true)
    dismissAuthNotice()
  })

  it('desabilita tudo quando é a própria publicação do usuário', () => {
    const wrapper = mountReactionBar({ authorId: viewerId, selectedReactions: [] })

    expect(wrapper.text()).toContain('Você não pode reagir à própria publicação.')
    const button = wrapper.findAll('button').find((b) => b.text().includes('Eu comeria'))!
    expect(button.attributes('disabled')).toBeDefined()
    expect(wrapper.get('button.reaction-bar__more').attributes('disabled')).toBeDefined()
  })
})
