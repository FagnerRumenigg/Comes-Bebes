import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll } from 'vitest'

import { mockHandlers } from '@/mocks/handlers'
import { backendStatus } from '@/composables/useBackendStatus'

export const mockServer = setupServer(...mockHandlers)

// 'warn', não 'error': alguns componentes deixam timers/queries em segundo
// plano rodando depois que o teste termina (sem wrapper.unmount()) — a
// requisição real que eles disparam depois não deve derrubar a suíte inteira,
// só ficar visível no log pra quem for investigar aquele teste específico.
beforeAll(() => mockServer.listen({ onUnhandledRequest: 'warn' }))

afterEach(() => {
  mockServer.resetHandlers()
  document.body.replaceChildren()
  backendStatus.offline = false
})

afterAll(() => mockServer.close())
