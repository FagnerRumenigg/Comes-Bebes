import { delay, http, HttpResponse } from 'msw'

import type { ApiErrorResponse, ChangePasswordRequest, UpdateUserRequest } from '@/api/generated/models'
import { mockAuthenticatedUsername } from '@/mocks/authentication'
import { accounts } from '@/mocks/handlers/auth'

function apiError(
  status: number,
  code: string,
  message: string,
  fieldErrors?: Record<string, string>,
): HttpResponse<ApiErrorResponse> {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      error: status === 401 ? 'Unauthorized' : status === 409 ? 'Conflict' : 'Bad Request',
      code,
      message,
      fieldErrors,
    },
    { status },
  )
}

function findAccountById(id: string): (typeof accounts)[number] | undefined {
  return accounts.find((candidate) => candidate.userId === id)
}

export const usersMockHandlers = [
  http.patch('*/users/:id', async ({ params, request }) => {
    await delay(220)
    const account = findAccountById(String(params.id))
    if (!account) return apiError(404, 'USER_NOT_FOUND', 'Usuário não encontrado.')

    const body = (await request.json()) as UpdateUserRequest
    if (body.displayName) account.displayName = body.displayName
    if (body.username?.trim()) account.username = body.username.trim()
    if (body.email !== undefined) account.email = body.email

    return HttpResponse.json({
      id: account.userId,
      username: account.username,
      displayName: account.displayName,
      role: account.role,
      status: 'ACTIVE',
      onboardingCompleted: account.onboardingCompleted,
      followedByCurrentUser: null,
    })
  }),

  http.patch('*/users/:id/password', async ({ params, request }) => {
    await delay(220)
    const account = findAccountById(String(params.id))
    if (!account) return apiError(404, 'USER_NOT_FOUND', 'Usuário não encontrado.')

    const body = (await request.json()) as ChangePasswordRequest
    if (body.currentPassword !== account.password) {
      return apiError(400, 'INVALID_CURRENT_PASSWORD', 'Senha atual inválida.', {
        currentPassword: 'Senha atual inválida.',
      })
    }
    account.password = body.newPassword
    return new HttpResponse(null, { status: 204 })
  }),

  http.patch('*/users/:id/onboarding', async ({ params }) => {
    await delay(220)
    const account = findAccountById(String(params.id))
    if (!account) return apiError(404, 'USER_NOT_FOUND', 'Usuário não encontrado.')

    account.onboardingCompleted = true
    return new HttpResponse(null, { status: 204 })
  }),

  http.delete('*/users/:id/account', async ({ params, request }) => {
    await delay(220)
    const requesterUsername = mockAuthenticatedUsername(request)
    const account = findAccountById(String(params.id))
    if (!account) return apiError(404, 'USER_NOT_FOUND', 'Usuário não encontrado.')
    if (requesterUsername !== account.username) {
      return apiError(403, 'FORBIDDEN', 'A conta autenticada não corresponde ao usuário informado.')
    }

    account.displayName = 'Conta apagada'
    account.email = null
    return new HttpResponse(null, { status: 204 })
  }),
]
