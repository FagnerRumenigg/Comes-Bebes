import axios, { type AxiosRequestConfig } from 'axios'

import { markBackendOffline, markBackendOnline } from '@/composables/useBackendStatus'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8082'
const unreachableStatuses = new Set([502, 503, 504])

function isBackendUnreachable(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false
  const status = error.response?.status
  // Sem response = falha de rede/timeout (o caso mais comum quando o
  // container está mesmo desligado). Um 502/503/504 cobre o caso em que o
  // ngrok responde no lugar do backend indisponível.
  return status === undefined || unreachableStatuses.has(status)
}
const defaultHeaders: Record<string, string> = {
  Accept: 'application/json',
}

export function requiresNgrokBrowserWarningBypass(apiBaseUrl: string): boolean {
  try {
    const hostname = new URL(apiBaseUrl, window.location.origin).hostname
    return hostname.endsWith('.ngrok-free.dev') || hostname.endsWith('.ngrok-free.app')
  } catch {
    return false
  }
}

if (requiresNgrokBrowserWarningBypass(baseURL)) {
  defaultHeaders['ngrok-skip-browser-warning'] = 'true'
}

export const httpClient = axios.create({
  baseURL,
  headers: defaultHeaders,
})

type AccessTokenProvider = () => string | null
type UnauthorizedHandler = () => Promise<boolean>
type RetriableRequestConfig = AxiosRequestConfig & { retriedAfterUnauthorized?: boolean }

let accessTokenProvider: AccessTokenProvider = () => null
let unauthorizedHandler: UnauthorizedHandler | null = null

function normalizeMultipartParts(data: unknown): unknown {
  if (typeof FormData === 'undefined' || !(data instanceof FormData)) return data

  const jsonPart = data.get('data')
  if (typeof jsonPart === 'string') {
    data.set('data', new Blob([jsonPart], { type: 'application/json' }))
  }

  return data
}

export function setAccessTokenProvider(provider: AccessTokenProvider): void {
  accessTokenProvider = provider
}

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

httpClient.interceptors.request.use((config) => {
  const accessToken = accessTokenProvider()
  const requestBaseUrl = new URL(config.baseURL ?? '/', window.location.origin)
  const requestUrl = new URL(config.url ?? '', requestBaseUrl).toString()

  if (requiresNgrokBrowserWarningBypass(requestUrl)) {
    config.headers['ngrok-skip-browser-warning'] = 'true'
  }

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => {
    markBackendOnline()
    return response
  },
  (error: unknown) => {
    if (isBackendUnreachable(error)) {
      markBackendOffline()
    } else if (axios.isAxiosError(error) && error.response) {
      // O servidor respondeu (mesmo com erro 4xx) - está alcançável.
      markBackendOnline()
    }
    return Promise.reject(error)
  },
)

httpClient.interceptors.response.use(undefined, async (error: unknown) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
    return Promise.reject(error)
  }

  const config = error.config as RetriableRequestConfig
  const isAuthenticationRequest = config.url?.startsWith('/auth/') ?? false
  if (isAuthenticationRequest || config.retriedAfterUnauthorized || !unauthorizedHandler) {
    return Promise.reject(error)
  }

  config.retriedAfterUnauthorized = true
  const renewed = await unauthorizedHandler()
  if (!renewed) return Promise.reject(error)

  return httpClient.request(config)
})

export async function apiRequest<T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> {
  const requestConfig = { ...config, ...options }
  requestConfig.data = normalizeMultipartParts(requestConfig.data)
  const response = await httpClient.request<T>(requestConfig)
  return response.data
}
