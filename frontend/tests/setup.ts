import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll } from 'vitest'

import { mockHandlers } from '@/mocks/handlers'

export const mockServer = setupServer(...mockHandlers)

beforeAll(() => mockServer.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  mockServer.resetHandlers()
  document.body.replaceChildren()
})

afterAll(() => mockServer.close())
