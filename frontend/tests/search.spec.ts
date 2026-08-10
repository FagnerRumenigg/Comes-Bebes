import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SearchView from '@/views/SearchView.vue'

describe('SearchView', () => {
  it('desabilita o botão de busca enquanto nenhum termo for informado', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: 0 } },
    })
    const wrapper = mount(SearchView, {
      global: {
        plugins: [[VueQueryPlugin, { queryClient }]],
      },
    })

    const submitButton = wrapper.get('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Informe um título ou ingrediente para começar.')
  })
})
