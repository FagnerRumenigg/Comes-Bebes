import { setupWorker } from 'msw/browser'

import { mockHandlers } from './handlers'

export const mockWorker = setupWorker(...mockHandlers)

export async function startMockServer(): Promise<void> {
  await mockWorker.start({
    onUnhandledRequest: 'bypass',
    quiet: true,
  })
}
