import type { NavigationGuardReturn, RouteLocationNormalized } from 'vue-router'

import type { UserResponseRole } from '@/api/generated/models/userResponseRole'

export interface RouteSession {
  authenticated: boolean
  role: UserResponseRole | null
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

  if (access === 'admin' && session.role !== 'ADMIN') {
    return { name: 'feed' }
  }

  return true
}

export async function routeAccessGuard(
  to: RouteLocationNormalized,
): Promise<NavigationGuardReturn> {
  return resolveRouteAccess(to, await resolveSession())
}
