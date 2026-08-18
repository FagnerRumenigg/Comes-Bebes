import { delay, http, HttpResponse } from 'msw'

import { getFollowersMockHandler, getFollowingMockHandler } from '@/api/generated/users/users.msw'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { setFollowingMock } from '@/mocks/state/follows'

// GET /users/:id (perfil), /users/:id/publications e /users/:id/notifications já têm mocks
// próprios com lógica real em discovery.ts. followers/following ficam com o mock gerado
// (dados aleatórios) por enquanto — nenhuma tela consome o conteúdo da lista ainda.
export const followMockHandlers = [
  http.put('*/users/:id/follow', async ({ params, request }) => {
    await delay(150)
    const viewerUsername = mockAuthenticatedUsername(request)
    if (!viewerUsername) {
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    }
    setFollowingMock(viewerUsername, params.id as string, true)
    return new HttpResponse(null, { status: 204 })
  }),
  http.delete('*/users/:id/follow', async ({ params, request }) => {
    await delay(150)
    const viewerUsername = mockAuthenticatedUsername(request)
    if (!viewerUsername) {
      return HttpResponse.json({ message: 'Autenticação necessária.' }, { status: 401 })
    }
    setFollowingMock(viewerUsername, params.id as string, false)
    return new HttpResponse(null, { status: 204 })
  }),
  getFollowersMockHandler(),
  getFollowingMockHandler(),
]
