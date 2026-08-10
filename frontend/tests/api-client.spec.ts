import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'

import {
  apiRequest,
  requiresNgrokBrowserWarningBypass,
  setAccessTokenProvider,
  setUnauthorizedHandler,
} from '@/api/client'
import { mockServer } from './setup'

afterEach(() => {
  setAccessTokenProvider(() => null)
  setUnauthorizedHandler(null)
})

describe('cliente HTTP autenticado', () => {
  it('ativa o bypass somente para endpoints gratuitos do Ngrok', () => {
    expect(
      requiresNgrokBrowserWarningBypass(
        'https://ungraded-audition-ending.ngrok-free.dev/comesebebes',
      ),
    ).toBe(true)
    expect(requiresNgrokBrowserWarningBypass('https://api.example.com/comesebebes')).toBe(false)
    expect(requiresNgrokBrowserWarningBypass('/comesebebes')).toBe(false)
  })

  it('envia o bypass também em uma URL absoluta de imagem do Ngrok', async () => {
    mockServer.use(
      http.get('https://images.ngrok-free.dev/comesebebes/images/prato.webp', ({ request }) =>
        HttpResponse.json({ bypass: request.headers.get('ngrok-skip-browser-warning') }),
      ),
    )

    const response = await apiRequest<{ bypass: string }>({
      url: 'https://images.ngrok-free.dev/comesebebes/images/prato.webp',
      method: 'GET',
    })

    expect(response.bypass).toBe('true')
  })

  it('renova a sessão uma vez e repete uma requisição que recebeu 401', async () => {
    let accessToken = 'expired-token'
    let renewals = 0
    const receivedTokens: string[] = []
    setAccessTokenProvider(() => accessToken)
    setUnauthorizedHandler(async () => {
      renewals += 1
      accessToken = 'renewed-token'
      return true
    })
    mockServer.use(
      http.get('*/test/protected', ({ request }) => {
        const token = request.headers.get('Authorization') ?? ''
        receivedTokens.push(token)
        if (token === 'Bearer expired-token') {
          return HttpResponse.json({ message: 'Token expirado' }, { status: 401 })
        }
        return HttpResponse.json({ ok: true })
      }),
    )

    const response = await apiRequest<{ ok: boolean }>({ url: '/test/protected', method: 'GET' })

    expect(response).toEqual({ ok: true })
    expect(renewals).toBe(1)
    expect(receivedTokens).toEqual(['Bearer expired-token', 'Bearer renewed-token'])
  })

  it('não tenta renovar novamente quando a repetição também recebe 401', async () => {
    let renewals = 0
    setAccessTokenProvider(() => 'invalid-token')
    setUnauthorizedHandler(async () => {
      renewals += 1
      return true
    })
    mockServer.use(
      http.get('*/test/always-unauthorized', () =>
        HttpResponse.json({ message: 'Não autorizado' }, { status: 401 }),
      ),
    )

    await expect(
      apiRequest({ url: '/test/always-unauthorized', method: 'GET' }),
    ).rejects.toMatchObject({ response: { status: 401 } })
    expect(renewals).toBe(1)
  })

  it('envia a parte data do multipart como JSON', async () => {
    let receivedType = ''
    let receivedData: unknown
    mockServer.use(
      http.post('*/test/multipart', async ({ request }) => {
        const formData = await request.formData()
        const data = formData.get('data')
        if (data && typeof data !== 'string') {
          receivedType = data.type
          receivedData = JSON.parse(await data.text())
        }
        return HttpResponse.json({ ok: true })
      }),
    )
    const formData = new FormData()
    formData.append('data', JSON.stringify({ type: 'DISH' }))
    formData.append('image', new File(['image'], 'dish.png', { type: 'image/png' }))

    await apiRequest({
      url: '/test/multipart',
      method: 'POST',
      headers: { 'Content-Type': 'multipart/form-data' },
      data: formData,
    })

    expect(receivedType).toBe('application/json')
    expect(receivedData).toEqual({ type: 'DISH' })
  })
})
