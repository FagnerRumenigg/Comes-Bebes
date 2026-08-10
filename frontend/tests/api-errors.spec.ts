import { describe, expect, it } from 'vitest'

import { normalizeHttpError, shouldRetryQuery } from '@/api/errors'

const axiosError = (status: number) => ({
  isAxiosError: true,
  response: { status },
})

describe('política de repetição da API', () => {
  it('não repete erros HTTP do cliente', () => {
    expect(shouldRetryQuery(0, axiosError(400))).toBe(false)
    expect(shouldRetryQuery(0, axiosError(404))).toBe(false)
    expect(shouldRetryQuery(0, axiosError(429))).toBe(false)
  })

  it('repete uma vez falhas transitórias', () => {
    expect(shouldRetryQuery(0, axiosError(500))).toBe(true)
    expect(shouldRetryQuery(0, new Error('Falha de rede'))).toBe(true)
    expect(shouldRetryQuery(1, axiosError(500))).toBe(false)
  })
})

describe('normalização de erros HTTP', () => {
  it.each([
    [400, 'Revise os dados informados.'],
    [401, 'Sua sessão expirou. Entre novamente.'],
    [403, 'Você não tem permissão para realizar esta ação.'],
    [409, 'Os dados informados entram em conflito com um registro existente.'],
    [429, 'Muitas tentativas em pouco tempo. Aguarde e tente novamente.'],
  ])('fornece uma mensagem padrão para o status %i', (status, expectedMessage) => {
    expect(normalizeHttpError({ ...axiosError(status), message: 'Request failed' }).message).toBe(
      expectedMessage,
    )
  })

  it('preserva a mensagem e os erros por campo enviados pela API', () => {
    const normalized = normalizeHttpError({
      ...axiosError(400),
      message: 'Request failed',
      response: {
        status: 400,
        data: {
          code: 'VALIDATION_ERROR',
          message: 'Revise os campos.',
          fieldErrors: { username: 'Nome indisponível.' },
        },
      },
    })

    expect(normalized).toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Revise os campos.',
      fieldErrors: { username: 'Nome indisponível.' },
    })
  })
})
