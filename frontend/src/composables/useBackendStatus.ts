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
// Limiar maior e separado pra tela cheia (BackendOfflineScreen, a "tela de
// carregando"): 3s é normal pra uma requisição comum meio lenta (rede ruim,
// consulta pesada) e não deve tomar a tela inteira anunciando "o servidor
// tá ligando". Só depois de 8s sem resposta é que provavelmente é mesmo um
// scale-to-zero do Azure Container Apps acordando (esse caso não gera erro
// de rede - a requisição só fica na fila até a instância subir -, então
// esperar pelo catch do interceptor, como markBackendOffline() fazia antes,
// nunca disparava a tela nesse cenário).
const BACKEND_OFFLINE_SCREEN_THRESHOLD_MS = 8_000

let pendingRequestCount = 0
let slowRequestTimer: ReturnType<typeof setTimeout> | undefined
let backendOfflineScreenTimer: ReturnType<typeof setTimeout> | undefined

export function markRequestStarted(): void {
  pendingRequestCount += 1
  if (pendingRequestCount === 1) {
    if (!slowRequestTimer) {
      slowRequestTimer = setTimeout(() => {
        if (pendingRequestCount > 0) backendStatus.slowRequest = true
      }, SLOW_REQUEST_THRESHOLD_MS)
    }
    if (!backendOfflineScreenTimer) {
      backendOfflineScreenTimer = setTimeout(() => {
        if (pendingRequestCount > 0) markBackendOffline()
      }, BACKEND_OFFLINE_SCREEN_THRESHOLD_MS)
    }
  }
}

export function markRequestFinished(): void {
  pendingRequestCount = Math.max(0, pendingRequestCount - 1)
  if (pendingRequestCount === 0) {
    if (slowRequestTimer) {
      clearTimeout(slowRequestTimer)
      slowRequestTimer = undefined
    }
    if (backendOfflineScreenTimer) {
      clearTimeout(backendOfflineScreenTimer)
      backendOfflineScreenTimer = undefined
    }
    backendStatus.slowRequest = false
  }
}
