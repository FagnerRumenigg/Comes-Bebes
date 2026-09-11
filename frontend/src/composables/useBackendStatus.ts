import { reactive } from 'vue'

// A tela de aquecimento comunica o cold start do backend remoto. Pode ser
// desativada no desenvolvimento local sem alterar o comportamento de testes
// ou de outros ambientes.
export const backendOfflineScreenEnabled =
  import.meta.env.VITE_ENABLE_BACKEND_OFFLINE_SCREEN !== 'false'

export const backendStatus = reactive({
  offline: false,
  slowRequest: false,
  preserveCurrentView: false,
})

export function markBackendOffline(): void {
  backendStatus.offline = true
}

export function markBackendOnline(): void {
  if (backendStatus.offline) backendStatus.offline = false
  backendStatus.preserveCurrentView = false
}

export function preserveCurrentViewWhileRequesting(): void {
  backendStatus.preserveCurrentView = true
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
const BACKEND_READINESS_CHECK_DELAY_MS = 8_000
const BACKEND_READINESS_CHECK_TIMEOUT_MS = 4_000

let pendingRequestCount = 0
let slowRequestTimer: ReturnType<typeof setTimeout> | undefined
let backendReadinessTimer: ReturnType<typeof setTimeout> | undefined
let readinessCheck: (() => Promise<boolean>) | undefined

export function setBackendReadinessCheck(check: () => Promise<boolean>): void {
  readinessCheck = check
}

async function verifyBackendReadiness(): Promise<void> {
  if (pendingRequestCount === 0 || !readinessCheck) return
  const available = await readinessCheck()
  if (pendingRequestCount > 0 && !available) markBackendOffline()
}

export function markRequestStarted(): void {
  pendingRequestCount += 1
  if (pendingRequestCount === 1) {
    if (!slowRequestTimer) {
      slowRequestTimer = setTimeout(() => {
        if (pendingRequestCount > 0) backendStatus.slowRequest = true
      }, SLOW_REQUEST_THRESHOLD_MS)
    }
    if (!backendReadinessTimer) {
      backendReadinessTimer = setTimeout(() => {
        backendReadinessTimer = undefined
        void verifyBackendReadiness()
      }, BACKEND_READINESS_CHECK_DELAY_MS)
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
    if (backendReadinessTimer) {
      clearTimeout(backendReadinessTimer)
      backendReadinessTimer = undefined
    }
    backendStatus.slowRequest = false
  }
}
