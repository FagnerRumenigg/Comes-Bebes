import { reactive } from 'vue'

export const backendStatus = reactive({
  offline: false,
})

export function markBackendOffline(): void {
  backendStatus.offline = true
}

export function markBackendOnline(): void {
  if (backendStatus.offline) backendStatus.offline = false
}
