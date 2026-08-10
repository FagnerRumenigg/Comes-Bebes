import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import App from '@/App.vue'
import FoundationView from '@/views/FoundationView.vue'

describe('fundação da aplicação', () => {
  it('inicializa Vue Router, Pinia e Vue Query', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: FoundationView }],
    })
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    await router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient }]],
      },
    })

    expect(wrapper.get('h1').text()).toBe('Fundação técnica pronta')
    expect(wrapper.findAll('dd').map((item) => item.text())).toEqual(['ativo', 'ativo'])
  })
})
