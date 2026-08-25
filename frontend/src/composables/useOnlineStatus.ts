import { reactive } from 'vue'

export const onlineStatus = reactive({
  online: typeof navigator === 'undefined' ? true : navigator.onLine,
})

export function initializeOnlineStatus(): void {
  if (typeof window === 'undefined') return

  window.addEventListener('online', () => {
    onlineStatus.online = true
  })
  window.addEventListener('offline', () => {
    onlineStatus.online = false
  })
}
