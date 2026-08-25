import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ErrorScreen from '@/components/layout/ErrorScreen.vue'

describe('ErrorScreen', () => {
  it('mostra a causa "offline" sem o botão de voltar ao início', () => {
    const wrapper = mount(ErrorScreen, { props: { cause: 'offline' } })

    expect(wrapper.get('h1').text()).toBe('Você está sem internet')
    expect(wrapper.text()).toContain('Parece que a conexão caiu.')
    expect(wrapper.text()).toContain('Nada do que você escreveu foi perdido.')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual(['Tentar de novo'])
  })

  it('mostra a causa "server" com o botão de voltar ao início', () => {
    const wrapper = mount(ErrorScreen, { props: { cause: 'server' } })

    expect(wrapper.get('h1').text()).toBe('A cozinha deu uma engasgada')
    expect(wrapper.text()).toContain('O problema é nosso, não seu.')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual([
      'Tentar de novo',
      'Voltar ao início',
    ])
  })

  it('mostra a causa "unknown" com o botão de voltar ao início', () => {
    const wrapper = mount(ErrorScreen, { props: { cause: 'unknown' } })

    expect(wrapper.get('h1').text()).toBe('Alguma coisa entornou aqui')
    expect(wrapper.text()).toContain('Aconteceu um erro inesperado.')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual([
      'Tentar de novo',
      'Voltar ao início',
    ])
  })

  it('emite retry e home ao clicar nos botões', async () => {
    const wrapper = mount(ErrorScreen, { props: { cause: 'server' } })

    await wrapper.findAll('button')[0]!.trigger('click')
    await wrapper.findAll('button')[1]!.trigger('click')

    expect(wrapper.emitted('retry')).toHaveLength(1)
    expect(wrapper.emitted('home')).toHaveLength(1)
  })
})
