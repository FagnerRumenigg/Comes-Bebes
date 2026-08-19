import { reactive } from 'vue'

export const backendStatus = reactive({
  offline: false,
  slowRequest: false,
})

export function markBackendOffline(): void {
  backendStatus.offline = true
}

export function markBackendOnline(): void {
  if (backendStatus.offline) backendStatus.offline = false
}

const SLOW_REQUEST_THRESHOLD_MS = 3_000

let pendingRequestCount = 0
let slowRequestTimer: ReturnType<typeof setTimeout> | undefined

/**
 * Chamado quando uma requisição HTTP começa. Se ainda estiver pendente depois
 * de SLOW_REQUEST_THRESHOLD_MS, marca slowRequest=true - usado pra avisar o
 * usuário que o servidor pode estar acordando de um scale-to-zero, em vez de
 * deixar a UI travada sem explicação.
 */
export function markRequestStarted(): void {
  pendingRequestCount += 1
  if (pendingRequestCount === 1 && !slowRequestTimer) {
    slowRequestTimer = setTimeout(() => {
      if (pendingRequestCount > 0) backendStatus.slowRequest = true
    }, SLOW_REQUEST_THRESHOLD_MS)
  }
}

export function markRequestFinished(): void {
  pendingRequestCount = Math.max(0, pendingRequestCount - 1)
  if (pendingRequestCount === 0) {
    if (slowRequestTimer) {
      clearTimeout(slowRequestTimer)
      slowRequestTimer = undefined
    }
    backendStatus.slowRequest = false
  }
}
