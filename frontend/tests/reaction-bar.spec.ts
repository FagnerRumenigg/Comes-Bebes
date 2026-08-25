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
      emailRequired: false,
    }
  }

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
  return mount(ReactionBar, {
    props: { publication: { ...mockPublications[0]!, ...publication } },
    global: { plugins: [pinia, [VueQueryPlugin, { queryClient }]] },
  })
}

describe('ReactionBar', () => {
  it('mostra as reações já usadas e o botão Reagir, sem número', () => {
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      usedReactions: ['HUNGRY', 'BEAUTIFUL'],
      selectedReactions: [],
    })

    const badges = wrapper.findAll('.reaction-bar__badge')
    expect(badges.map((badge) => badge.text())).toEqual(['Me deu fome', 'Ficou lindo'])
    expect(wrapper.get('.reaction-bar__reagir').text()).toBe('Reagir')
    // Nenhum contador público em lugar nenhum do produto (produto5.md v5 §3.1) —
    // as reações no cartão são só presença, nunca número.
    badges.forEach((badge) => expect(badge.text()).not.toMatch(/\d/))
  })

  it('reação sem nenhum uso não aparece como emblema no cartão', () => {
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      usedReactions: [],
      selectedReactions: [],
    })

    expect(wrapper.findAll('.reaction-bar__badge')).toHaveLength(0)
  })

  it('abre o seletor ao clicar em Reagir e alterna uma reação', async () => {
    mockServer.use(http.put('*/publications/:id/reactions', () => new HttpResponse(null, { status: 204 })))
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      usedReactions: [],
      selectedReactions: [],
    })

    await wrapper.get('button.reaction-bar__reagir').trigger('click')
    const button = wrapper.findAll('button').find((b) => b.text() === 'Me deu fome')!
    await button.trigger('click')

    expect(button.attributes('aria-pressed')).toBe('true')
    expect(wrapper.text()).toContain('1 de 3 escolhidas')
  })

  it('explica em vez de ignorar ao tentar uma quarta reação', async () => {
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      selectedReactions: ['HUNGRY', 'BEAUTIFUL', 'PERFECT_COMBO'],
    })

    await wrapper.get('button.reaction-bar__reagir').trigger('click')
    const fourth = wrapper.findAll('button').find((b) => b.text() === 'Nunca provei')!
    await fourth.trigger('click')

    expect(wrapper.text()).toContain('Você já escolheu 3. Tire uma para poder escolher outra.')
    expect(fourth.attributes('aria-pressed')).toBe('false')
  })

  it('visitante aciona aviso de autenticação ao tentar reagir', async () => {
    dismissAuthNotice()
    const wrapper = mountReactionBar({ authorId: 'outro-usuario', selectedReactions: [] }, false)

    await wrapper.get('button.reaction-bar__reagir').trigger('click')

    expect(authNotice.visible).toBe(true)
    dismissAuthNotice()
  })

  it('desabilita tudo quando é a própria publicação do usuário', () => {
    const wrapper = mountReactionBar({ authorId: viewerId, selectedReactions: [] })

    expect(wrapper.text()).toContain('Você não pode reagir à própria publicação.')
    expect(wrapper.get('button.reaction-bar__reagir').attributes('disabled')).toBeDefined()
  })

  it('mostra erro e desfaz a seleção quando a chamada falha', async () => {
    mockServer.use(
      http.put('*/publications/:id/reactions', () =>
        HttpResponse.json({ code: 'ERROR', message: 'Não foi possível salvar.' }, { status: 400 }),
      ),
    )
    const wrapper = mountReactionBar({
      authorId: 'outro-usuario',
      usedReactions: [],
      selectedReactions: [],
    })

    await wrapper.get('button.reaction-bar__reagir').trigger('click')
    const button = wrapper.findAll('button').find((b) => b.text() === 'Me deu fome')!
    await button.trigger('click')

    await vi.waitFor(() => expect(button.attributes('aria-pressed')).toBe('false'))
    expect(wrapper.text()).toContain('Não foi possível salvar.')
  })
})
