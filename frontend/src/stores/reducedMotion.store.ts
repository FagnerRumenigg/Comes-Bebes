import { defineStore } from 'pinia'
import { ref } from 'vue'

export const REDUCED_MOTION_STORAGE_KEY = 'comes-e-bebes:reduce-motion'

function readStoredPreference(): boolean {
  if (typeof window === 'undefined') return false

  try {
    return window.localStorage.getItem(REDUCED_MOTION_STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

export const useReducedMotionStore = defineStore('reducedMotion', () => {
  const enabled = ref(false)

  function applyPreference(): void {
    if (typeof document === 'undefined') return
    document.documentElement.toggleAttribute('data-reduce-motion', enabled.value)
  }

  function persistPreference(): void {
    if (typeof window === 'undefined') return

    try {
      window.localStorage.setItem(REDUCED_MOTION_STORAGE_KEY, String(enabled.value))
    } catch {
      // A preferência continua válida durante a sessão quando o storage está indisponível.
    }
  }

  function setEnabled(value: boolean): void {
    enabled.value = value
    persistPreference()
    applyPreference()
  }

  function initialize(): void {
    enabled.value = readStoredPreference()
    applyPreference()
  }

  return {
    enabled,
    initialize,
    setEnabled,
  }
})
