import { delay, http, HttpResponse } from 'msw'

import type {
  ApiErrorResponse,
  CreateUserRequest,
  CreatedUserResponse,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
} from '@/api/generated/models'
import { mockAccounts } from '@/mocks/fixtures/auth'

type MockAccount = (typeof mockAccounts)[number]

const accounts: Array<{
  userId: string
  displayName: string
  username: string
  password: string
  role: MockAccount['role']
  onboardingCompleted: boolean
}> = mockAccounts.map((account) => ({ ...account, onboardingCompleted: true }))

const refreshSessions = new Map<string, string>()

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

function createSession(account: (typeof accounts)[number]): LoginResponse {
  const nonce = crypto.randomUUID()
  const refreshToken = `mock-refresh-${account.username}-${nonce}`
  refreshSessions.set(refreshToken, account.username)

  return {
    accessToken: `mock-access-${account.username}-${nonce}`,
    refreshToken,
    tokenType: 'Bearer',
    expiresInSeconds: 3600,
    userId: account.userId,
    username: account.username,
    role: account.role,
    onboardingCompleted: account.onboardingCompleted,
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  }
}

export const authMockHandlers = [
  http.post('*/auth/register', async ({ request }) => {
    await delay(250)
    const body = (await request.json()) as CreateUserRequest
    const fieldErrors: Record<string, string> = {}

    if (!body.displayName?.trim()) {
      fieldErrors.displayName = 'Informe seu nome de exibição.'
    } else if (body.displayName.trim().length > 100) {
      fieldErrors.displayName = 'Use no máximo 100 caracteres.'
    }
    if (!/^[a-zA-Z0-9_]{3,30}$/.test(body.username ?? '')) {
      fieldErrors.username = 'Use de 3 a 30 letras, números ou underscore.'
    }
    if ((body.password?.length ?? 0) < 8 || body.password.length > 72) {
      fieldErrors.password = 'A senha deve ter entre 8 e 72 caracteres.'
    }
    if (Object.keys(fieldErrors).length > 0) {
      return apiError(400, 'VALIDATION_ERROR', 'Revise os campos informados.', fieldErrors)
    }
    if (
      accounts.some((account) => account.username.toLowerCase() === body.username.toLowerCase())
    ) {
      return apiError(409, 'USERNAME_ALREADY_EXISTS', 'Este nome de usuário já está em uso.', {
        username: 'Escolha outro nome de usuário.',
      })
    }

    const account = {
      userId: crypto.randomUUID(),
      displayName: body.displayName.trim(),
      username: body.username,
      password: body.password,
      role: 'USER' as const,
      onboardingCompleted: false,
    }
    accounts.push(account)

    const response: CreatedUserResponse = {
      id: account.userId,
      displayName: account.displayName,
      username: account.username,
      role: account.role,
      showReactionCounts: true,
    }
    return HttpResponse.json(response, { status: 201 })
  }),

  http.post('*/auth/login', async ({ request }) => {
    await delay(250)
    const body = (await request.json()) as LoginRequest
    const account = accounts.find(
      (candidate) =>
        candidate.username.toLowerCase() === body.username.toLowerCase() &&
        candidate.password === body.password,
    )

    if (!account) {
      return apiError(401, 'INVALID_CREDENTIALS', 'Usuário ou senha inválidos.')
    }

    return HttpResponse.json(createSession(account))
  }),

  http.post('*/auth/refresh', async ({ request }) => {
    await delay(150)
    const body = (await request.json()) as RefreshTokenRequest
    const username =
      refreshSessions.get(body.refreshToken) ??
      /^mock-refresh-([a-zA-Z0-9_]+)-/.exec(body.refreshToken)?.[1]
    const account = accounts.find((candidate) => candidate.username === username)

    if (!account) {
      return apiError(401, 'INVALID_REFRESH_TOKEN', 'Sua sessão expirou. Entre novamente.')
    }

    refreshSessions.delete(body.refreshToken)
    return HttpResponse.json(createSession(account))
  }),

  http.post('*/auth/logout', async ({ request }) => {
    await delay(100)
    const body = (await request.json()) as RefreshTokenRequest
    refreshSessions.delete(body.refreshToken)
    return new HttpResponse(null, { status: 204 })
  }),
]
