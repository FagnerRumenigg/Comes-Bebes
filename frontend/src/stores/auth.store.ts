import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  logout as logoutRequest,
  logoutAll as logoutAllRequest,
  refresh as refreshRequest,
} from '@/api/generated/authentication/authentication'
import type { LoginResponse, LoginResponseRole } from '@/api/generated/models'

export const AUTH_STORAGE_KEY = 'comes-e-bebes:session'

type SessionStatus = 'idle' | 'initializing' | 'anonymous' | 'authenticated'
type SessionIdentity = Pick<
  LoginResponse,
  'userId' | 'username' | 'role' | 'onboardingCompleted' | 'hasUnseenPatchNotes'
>

interface StoredSession extends SessionIdentity {
  refreshToken: string
  remember: boolean
}

function isRole(value: unknown): value is LoginResponseRole {
  return value === 'USER' || value === 'ADMIN'
}

function parseStoredSession(value: string | null): StoredSession | null {
  if (!value) return null

  try {
    const session = JSON.parse(value) as Partial<StoredSession>
    if (
      typeof session.refreshToken !== 'string' ||
      typeof session.userId !== 'string' ||
      typeof session.username !== 'string' ||
      typeof session.remember !== 'boolean' ||
      typeof session.onboardingCompleted !== 'boolean' ||
      typeof session.hasUnseenPatchNotes !== 'boolean' ||
      !isRole(session.role)
    ) {
      return null
    }
    return session as StoredSession
  } catch {
    return null
  }
}

function readStoredSession(): StoredSession | null {
  if (typeof window === 'undefined') return null

  try {
    return (
      parseStoredSession(window.localStorage.getItem(AUTH_STORAGE_KEY)) ??
      parseStoredSession(window.sessionStorage.getItem(AUTH_STORAGE_KEY))
    )
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const status = ref<SessionStatus>('idle')
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const identity = ref<SessionIdentity | null>(null)
  const rememberSession = ref(false)
  let refreshTimer: ReturnType<typeof setTimeout> | undefined
  let initializationPromise: Promise<void> | undefined
  let renewalPromise: Promise<boolean> | undefined
  let accessTokenExpiresAt = 0
  let monitoringStarted = false

  const initialized = computed(
    () => status.value === 'anonymous' || status.value === 'authenticated',
  )
  const authenticated = computed(() => status.value === 'authenticated' && identity.value !== null)
  const isAdmin = computed(() => authenticated.value && identity.value?.role === 'ADMIN')

  function clearRefreshTimer(): void {
    if (refreshTimer) clearTimeout(refreshTimer)
    refreshTimer = undefined
  }

  function clearStoredSession(): void {
    if (typeof window === 'undefined') return
    try {
      window.localStorage.removeItem(AUTH_STORAGE_KEY)
      window.sessionStorage.removeItem(AUTH_STORAGE_KEY)
    } catch {
      // A sessão em memória ainda é encerrada quando o storage está indisponível.
    }
  }

  function persistSession(): void {
    if (!identity.value || !refreshToken.value || typeof window === 'undefined') return

    const storedSession: StoredSession = {
      ...identity.value,
      refreshToken: refreshToken.value,
      remember: rememberSession.value,
    }

    try {
      clearStoredSession()
      const storage = rememberSession.value ? window.localStorage : window.sessionStorage
      storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(storedSession))
    } catch {
      // A sessão continua funcional em memória.
    }
  }

  function scheduleRefresh(response: LoginResponse): void {
    clearRefreshTimer()
    if (typeof window === 'undefined') return

    const expiresAt = Date.parse(response.expiresAt)
    const fallbackExpiration = Date.now() + response.expiresInSeconds * 1000
    const expiration =
      !Number.isNaN(expiresAt) && expiresAt > Date.now() ? expiresAt : fallbackExpiration
    accessTokenExpiresAt = expiration
    const refreshAt = expiration - 30_000
    const delay = Math.max(refreshAt - Date.now(), 1_000)
    refreshTimer = setTimeout(() => void renewSession(), delay)
  }

  function handleBrowserResume(): void {
    if (
      typeof document !== 'undefined' &&
      document.visibilityState === 'visible' &&
      authenticated.value &&
      accessTokenExpiresAt <= Date.now() + 30_000
    ) {
      void renewSession()
    }
  }

  function startSessionMonitoring(): void {
    if (monitoringStarted || typeof window === 'undefined') return
    document.addEventListener('visibilitychange', handleBrowserResume)
    window.addEventListener('pageshow', handleBrowserResume)
    window.addEventListener('online', handleBrowserResume)
    monitoringStarted = true
  }

  function acceptSession(response: LoginResponse, remember = rememberSession.value): void {
    accessToken.value = response.accessToken
    refreshToken.value = response.refreshToken
    identity.value = {
      userId: response.userId,
      username: response.username,
      role: response.role,
      onboardingCompleted: response.onboardingCompleted,
      hasUnseenPatchNotes: response.hasUnseenPatchNotes,
    }
    rememberSession.value = remember
    status.value = 'authenticated'
    persistSession()
    scheduleRefresh(response)
    startSessionMonitoring()
  }

  function clearSession(): void {
    clearRefreshTimer()
    accessToken.value = null
    refreshToken.value = null
    identity.value = null
    rememberSession.value = false
    accessTokenExpiresAt = 0
    status.value = 'anonymous'
    clearStoredSession()
  }

  async function renewSession(): Promise<boolean> {
    if (renewalPromise) return renewalPromise

    renewalPromise = performSessionRenewal()
    try {
      return await renewalPromise
    } finally {
      renewalPromise = undefined
    }
  }

  async function performSessionRenewal(): Promise<boolean> {
    const currentRefreshToken = refreshToken.value
    if (!currentRefreshToken) {
      clearSession()
      return false
    }

    try {
      const response = await refreshRequest({ refreshToken: currentRefreshToken })
      acceptSession(response, rememberSession.value)
      return true
    } catch {
      return await recoverFromStaleRefreshToken(currentRefreshToken)
    }
  }

  /**
   * O refresh token é de uso único. Se outra aba já o rotacionou antes desta
   * chamada, o storage guarda um token mais novo que o desta aba — nesse
   * caso a sessão continua válida e não deve ser encerrada; adotamos o token
   * mais recente e tentamos renovar com ele antes de desistir.
   */
  async function recoverFromStaleRefreshToken(failedRefreshToken: string): Promise<boolean> {
    const storedSession = readStoredSession()
    if (storedSession && storedSession.refreshToken !== failedRefreshToken) {
      refreshToken.value = storedSession.refreshToken
      identity.value = {
        userId: storedSession.userId,
        username: storedSession.username,
        role: storedSession.role,
        onboardingCompleted: storedSession.onboardingCompleted,
        hasUnseenPatchNotes: storedSession.hasUnseenPatchNotes,
      }
      rememberSession.value = storedSession.remember
      try {
        const response = await refreshRequest({ refreshToken: storedSession.refreshToken })
        acceptSession(response, rememberSession.value)
        return true
      } catch {
        // Cai para o logout abaixo: mesmo o token mais recente falhou.
      }
    }
    clearSession()
    return false
  }

  async function initialize(): Promise<void> {
    if (initialized.value) return
    if (initializationPromise) return initializationPromise

    initializationPromise = (async () => {
      status.value = 'initializing'
      const storedSession = readStoredSession()
      if (!storedSession) {
        status.value = 'anonymous'
        return
      }

      refreshToken.value = storedSession.refreshToken
      identity.value = {
        userId: storedSession.userId,
        username: storedSession.username,
        role: storedSession.role,
        onboardingCompleted: storedSession.onboardingCompleted,
        hasUnseenPatchNotes: storedSession.hasUnseenPatchNotes,
      }
      rememberSession.value = storedSession.remember
      await renewSession()
    })()

    try {
      await initializationPromise
    } finally {
      initializationPromise = undefined
    }
  }

  async function logout(): Promise<void> {
    const currentRefreshToken = refreshToken.value
    try {
      if (currentRefreshToken) await logoutRequest({ refreshToken: currentRefreshToken })
    } finally {
      clearSession()
    }
  }

  async function logoutAll(): Promise<void> {
    try {
      if (authenticated.value) await logoutAllRequest()
    } finally {
      clearSession()
    }
  }

  function dispose(): void {
    clearRefreshTimer()
    if (monitoringStarted && typeof window !== 'undefined') {
      document.removeEventListener('visibilitychange', handleBrowserResume)
      window.removeEventListener('pageshow', handleBrowserResume)
      window.removeEventListener('online', handleBrowserResume)
      monitoringStarted = false
    }
  }

  return {
    status,
    accessToken,
    refreshToken,
    identity,
    rememberSession,
    initialized,
    authenticated,
    isAdmin,
    acceptSession,
    clearSession,
    initialize,
    renewSession,
    logout,
    logoutAll,
    dispose,
  }
})
