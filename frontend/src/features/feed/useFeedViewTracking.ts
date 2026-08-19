import { onBeforeUnmount, watch } from 'vue'

import type { PublicationResponse } from '@/api/generated/models'
import { useMarkViewed } from '@/api/generated/publications/publications'
import { useAuthStore } from '@/stores/auth.store'

const VISIBILITY_THRESHOLD = 0.5
const VISIBLE_DURATION_MS = 1_000
const FLUSH_INTERVAL_MS = 5_000

type TrackedPublication = Pick<PublicationResponse, 'id' | 'viewedByCurrentUser'>

/**
 * Detecta quando uma publicação fica >=50% visível por ~1s e registra a
 * visualização em lote (nunca uma requisição por card). Reaproveita o mesmo
 * padrão de threshold do IntersectionObserver do infinite scroll do
 * FeedView.
 */
export function useFeedViewTracking() {
  const authStore = useAuthStore()
  console.log(
    '[view-tracking] composable inicializado, initialized=',
    authStore.initialized,
    'authenticated=',
    authStore.authenticated,
  )
  const markViewedMutation = useMarkViewed({
    mutation: {
      onSuccess: (_data, variables) => {
        console.log('[view-tracking] enviado com sucesso', variables.data.publicationIds)
      },
      onError: (error, variables) => {
        console.error('[view-tracking] falhou ao enviar', variables.data.publicationIds, error)
      },
    },
  })

  const pendingIds = new Set<string>()
  const sentIds = new Set<string>()
  const visibilityTimers = new Map<string, ReturnType<typeof setTimeout>>()
  const elementToId = new Map<Element, string>()
  // Cards vistos antes da sessão terminar de resolver (authStore.initialized)
  // - nem visitante confirmado, nem autenticado confirmado ainda. Retomados
  // pelo watch abaixo assim que resolver, em vez de gastar trabalho de
  // observação à toa para quem acaba sendo visitante.
  const deferredUntilSessionResolved: Array<{ element: Element; publication: TrackedPublication }> =
    []

  let observer: IntersectionObserver | null = null
  let flushTimer: ReturnType<typeof setInterval> | undefined

  function flush(): void {
    if (pendingIds.size === 0) {
      console.log('[view-tracking] flush chamado, nada pendente')
      return
    }
    const ids = Array.from(pendingIds)
    pendingIds.clear()
    ids.forEach((id) => sentIds.add(id))
    console.log('[view-tracking] enviando lote', ids)
    markViewedMutation.mutate({ data: { publicationIds: ids } })
  }

  function ensureFlushTimer(): void {
    if (flushTimer) return
    flushTimer = setInterval(flush, FLUSH_INTERVAL_MS)
  }

  function handleVisibilityChange(): void {
    if (document.visibilityState === 'hidden') flush()
  }

  if (typeof window !== 'undefined') {
    // Um reload de página (F5) ou fechar a aba não dispara onBeforeUnmount do
    // Vue - sem isso, visualizações pendentes que ainda não bateram o
    // intervalo de lote (ou o unmount por navegação in-app) se perdem.
    window.addEventListener('pagehide', flush)
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  function stopObservingElement(element: Element): void {
    observer?.unobserve(element)
    elementToId.delete(element)
  }

  function ensureObserver(): IntersectionObserver | null {
    if (observer) return observer
    if (typeof IntersectionObserver === 'undefined') return null

    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const id = elementToId.get(entry.target)
          if (!id) continue

          console.log(
            '[view-tracking] intersection',
            id,
            'isIntersecting=',
            entry.isIntersecting,
            'ratio=',
            entry.intersectionRatio,
          )
          if (entry.isIntersecting && entry.intersectionRatio >= VISIBILITY_THRESHOLD) {
            if (visibilityTimers.has(id)) continue
            visibilityTimers.set(
              id,
              setTimeout(() => {
                visibilityTimers.delete(id)
                pendingIds.add(id)
                ensureFlushTimer()
                stopObservingElement(entry.target)
                console.log('[view-tracking] marcado como visto (pendente)', id)
              }, VISIBLE_DURATION_MS),
            )
          } else {
            const timer = visibilityTimers.get(id)
            if (timer) {
              clearTimeout(timer)
              visibilityTimers.delete(id)
            }
          }
        }
      },
      { threshold: VISIBILITY_THRESHOLD },
    )
    return observer
  }

  function startObserving(element: Element, publication: TrackedPublication): void {
    const activeObserver = ensureObserver()
    if (!activeObserver) return

    elementToId.set(element, publication.id)
    activeObserver.observe(element)
  }

  function observeCard(element: Element | null, publication: TrackedPublication): void {
    if (!element) {
      console.log('[view-tracking] observeCard sem elemento', publication.id)
      return
    }
    if (publication.viewedByCurrentUser || sentIds.has(publication.id)) {
      console.log(
        '[view-tracking] observeCard ja visto, pulando',
        publication.id,
        'viewedByCurrentUser=',
        publication.viewedByCurrentUser,
      )
      return
    }

    if (!authStore.initialized) {
      // Sessão ainda resolvendo (ex.: /auth/refresh em voo) - adia em vez de
      // descartar ou de criar o observer sem saber se vale a pena.
      console.log('[view-tracking] sessao nao resolvida ainda, adiando', publication.id)
      deferredUntilSessionResolved.push({ element, publication })
      return
    }
    // Visitante confirmado: nunca observa - visualização não se aplica e
    // seria trabalho à toa (IntersectionObserver + timers por nada).
    if (!authStore.authenticated) {
      console.log('[view-tracking] visitante confirmado, nunca observa', publication.id)
      return
    }

    console.log('[view-tracking] observando card', publication.id)
    startObserving(element, publication)
  }

  const stopWatchingSession = watch(
    () => authStore.initialized,
    (isInitialized) => {
      if (!isInitialized) return
      stopWatchingSession()
      const deferred = deferredUntilSessionResolved.splice(0)
      if (!authStore.authenticated) return
      deferred.forEach(({ element, publication }) => startObserving(element, publication))
    },
  )

  onBeforeUnmount(() => {
    stopWatchingSession()
    observer?.disconnect()
    visibilityTimers.forEach((timer) => clearTimeout(timer))
    visibilityTimers.clear()
    if (flushTimer) clearInterval(flushTimer)
    if (typeof window !== 'undefined') {
      window.removeEventListener('pagehide', flush)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
    flush()
  })

  return { observeCard }
}
