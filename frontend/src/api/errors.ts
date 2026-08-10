import axios from 'axios'

export interface NormalizedHttpError {
  status: number | null
  code: string | null
  message: string
  fieldErrors: Record<string, string>
  cause: unknown
}

export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    if (status && status >= 400 && status < 500) return false
  }

  return failureCount < 1
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readString(record: Record<string, unknown>, key: string): string | null {
  const value = record[key]
  return typeof value === 'string' ? value : null
}

function readFieldErrors(record: Record<string, unknown>): Record<string, string> {
  const value = record.fieldErrors
  if (!isRecord(value)) return {}

  return Object.fromEntries(
    Object.entries(value).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  )
}

const defaultStatusMessages: Partial<Record<number, string>> = {
  400: 'Revise os dados informados.',
  401: 'Sua sessão expirou. Entre novamente.',
  403: 'Você não tem permissão para realizar esta ação.',
  409: 'Os dados informados entram em conflito com um registro existente.',
  429: 'Muitas tentativas em pouco tempo. Aguarde e tente novamente.',
}

export function normalizeHttpError(error: unknown): NormalizedHttpError {
  if (!axios.isAxiosError(error)) {
    return {
      status: null,
      code: null,
      message: error instanceof Error ? error.message : 'Erro inesperado.',
      fieldErrors: {},
      cause: error,
    }
  }

  const payload = isRecord(error.response?.data) ? error.response.data : {}
  const status = error.response?.status ?? null

  return {
    status,
    code: readString(payload, 'code'),
    message:
      readString(payload, 'message') ??
      (status ? defaultStatusMessages[status] : null) ??
      error.message,
    fieldErrors: readFieldErrors(payload),
    cause: error,
  }
}
