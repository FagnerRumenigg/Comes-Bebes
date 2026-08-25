import type { NavigationGuardReturn, RouteLocationNormalized } from 'vue-router'

import type { UserResponseRole } from '@/api/generated/models/userResponseRole'
import { WELCOME_SEEN_STORAGE_KEY } from '@/utils/storageKeys'

export interface RouteSession {
  authenticated: boolean
  role: UserResponseRole | null
  onboardingCompleted?: boolean
  emailRequired?: boolean
}

export type RouteSessionResolver = () => RouteSession | Promise<RouteSession>

const anonymousSession: RouteSession = {
  authenticated: false,
  role: null,
}

let resolveSession: RouteSessionResolver = () => anonymousSession

export function setRouteSessionResolver(resolver: RouteSessionResolver): void {
  resolveSession = resolver
}

export function resetRouteSessionResolver(): void {
  resolveSession = () => anonymousSession
}

export function resolveRouteAccess(
  to: RouteLocationNormalized,
  session: RouteSession,
): NavigationGuardReturn {
  const access = to.meta.access ?? 'public'

  if (access === 'guest' && session.authenticated) {
    return { name: 'feed' }
  }

  if ((access === 'authenticated' || access === 'admin') && !session.authenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  // Conta antiga sem e-mail (produto5.md v5 §5.1): pede o e-mail antes de qualquer outra
  // coisa, inclusive antes do onboarding — é sobre a credencial de entrar, não sobre conhecer
  // o produto.
  if (session.authenticated && to.name !== 'require-email' && session.emailRequired === true) {
    return { name: 'require-email' }
  }

  // Onboarding e boas-vindas são a mesma tela (docs/telas/01-boas-vindas-e-erro.html) -
  // conta recém-criada cai na mesma tela que um visitante de primeira vez veria.
  if (session.authenticated && to.name !== 'welcome' && session.onboardingCompleted === false) {
    return { name: 'welcome' }
  }

  if (access === 'admin' && session.role !== 'ADMIN') {
    return { name: 'feed' }
  }

  return true
}

function hasSeenWelcome(): boolean {
  try {
    return window.localStorage.getItem(WELCOME_SEEN_STORAGE_KEY) === 'true'
  } catch {
    return true
  }
}

export async function routeAccessGuard(
  to: RouteLocationNormalized,
): Promise<NavigationGuardReturn> {
  const session = await resolveSession()

  // Primeira visita de quem ainda não tem conta: mostra a tela de boas-vindas
  // antes do feed, uma vez por navegador (produto5.md/docs/telas/01).
  if (!session.authenticated && to.name === 'feed' && !hasSeenWelcome()) {
    return { name: 'welcome' }
  }

  return resolveRouteAccess(to, session)
}
