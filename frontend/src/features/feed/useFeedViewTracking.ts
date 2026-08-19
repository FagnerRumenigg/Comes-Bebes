import { onBeforeUnmount } from 'vue'

import type { PublicationResponse } from '@/api/generated/models'
import { useMarkViewed } from '@/api/generated/publications/publications'
import { useAuthStore } from '@/stores/auth.store'

const VISIBILITY_THRESHOLD = 0.5
const VISIBLE_DURATION_MS = 1_000
const FLUSH_INTERVAL_MS = 5_000

/**
 * Detecta quando uma publicação fica >=50% visível por ~1s e registra a
 * visualização em lote (nunca uma requisição por card). Reaproveita o mesmo
 * padrão de threshold do IntersectionObserver do infinite scroll do
 * FeedView.
 */
export function useFeedViewTracking() {
  const authStore = useAuthStore()
  const markViewedMutation = useMarkViewed()

  const pendingIds = new Set<string>()
  const sentIds = new Set<string>()
  const visibilityTimers = new Map<string, ReturnType<typeof setTimeout>>()
  const elementToId = new Map<Element, string>()

  let observer: IntersectionObserver | null = null
  let flushTimer: ReturnType<typeof setInterval> | undefined

  function flush(): void {
    if (pendingIds.size === 0) return
    const ids = Array.from(pendingIds)
    pendingIds.clear()
    ids.forEach((id) => sentIds.add(id))
    markViewedMutation.mutate({ data: { publicationIds: ids } })
  }

  function ensureFlushTimer(): void {
    if (flushTimer) return
    flushTimer = setInterval(flush, FLUSH_INTERVAL_MS)
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

          if (entry.isIntersecting && entry.intersectionRatio >= VISIBILITY_THRESHOLD) {
            if (visibilityTimers.has(id)) continue
            visibilityTimers.set(
              id,
              setTimeout(() => {
                visibilityTimers.delete(id)
                pendingIds.add(id)
                ensureFlushTimer()
                stopObservingElement(entry.target)
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

  function observeCard(
    element: Element | null,
    publication: Pick<PublicationResponse, 'id' | 'viewedByCurrentUser'>,
  ): void {
    if (!element || !authStore.authenticated) return
    if (publication.viewedByCurrentUser || sentIds.has(publication.id)) return

    const activeObserver = ensureObserver()
    if (!activeObserver) return

    elementToId.set(element, publication.id)
    activeObserver.observe(element)
  }

  onBeforeUnmount(() => {
    observer?.disconnect()
    visibilityTimers.forEach((timer) => clearTimeout(timer))
    visibilityTimers.clear()
    if (flushTimer) clearInterval(flushTimer)
    flush()
  })

  return { observeCard }
}
