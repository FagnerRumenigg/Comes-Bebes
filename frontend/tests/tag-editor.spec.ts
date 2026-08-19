import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import type { TagResponse } from '@/api/generated/models'
import TagEditor from '@/components/publication/TagEditor.vue'
import { mockServer } from './setup'

function mountEditor(modelValue: string[] = []) {
  return mount(TagEditor, {
    props: { modelValue },
    global: {
      plugins: [[VueQueryPlugin, { queryClient: new QueryClient() }]],
    },
  })
}

describe('TagEditor', () => {
  it('adiciona uma tag ao pressionar Enter', async () => {
    const wrapper = mountEditor([])

    await wrapper.get('input').setValue('Vegano')
    await wrapper.get('input').trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['Vegano']])
  })

  it('não adiciona tag duplicada (sem diferenciar maiúscula/minúscula)', async () => {
    const wrapper = mountEditor(['Vegano'])

    await wrapper.get('input').setValue('vegano')
    await wrapper.get('input').trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('desabilita o campo ao atingir 5 tags', () => {
    const wrapper = mountEditor(['a', 'b', 'c', 'd', 'e'])

    expect(wrapper.get('input').attributes('disabled')).toBeDefined()
  })

  it('remove uma tag ao clicar no botão de remover', async () => {
    const wrapper = mountEditor(['Vegano', 'Doce'])

    await wrapper.get('button[aria-label="Remover tag Vegano"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['Doce']])
  })

  it('busca sugestões e adiciona ao clicar em uma delas', async () => {
    const suggestion: TagResponse = { name: 'Vegano', slug: 'vegano', official: true }
    mockServer.use(http.get('*/tags/search', () => HttpResponse.json([suggestion])))

    const wrapper = mountEditor([])
    await wrapper.get('input').setValue('veg')

    await new Promise((resolve) => setTimeout(resolve, 400))
    await wrapper.vm.$nextTick()
    await new Promise((resolve) => setTimeout(resolve, 50))
    await wrapper.vm.$nextTick()

    const suggestionButton = wrapper.find('.tag-editor__suggestions button')
    expect(suggestionButton.exists()).toBe(true)
    expect(suggestionButton.text()).toContain('Vegano')

    await suggestionButton.trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['Vegano']])
  }, 10_000)
})
