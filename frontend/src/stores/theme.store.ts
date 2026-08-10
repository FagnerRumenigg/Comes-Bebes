import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export type ThemePreference = 'light' | 'dark' | 'system'
export type ResolvedTheme = Exclude<ThemePreference, 'system'>

export const THEME_STORAGE_KEY = 'comes-e-bebes:theme'
const DARK_THEME_QUERY = '(prefers-color-scheme: dark)'

function isThemePreference(value: string | null): value is ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system'
}

function readStoredPreference(): ThemePreference {
  if (typeof window === 'undefined') return 'system'

  try {
    const storedPreference = window.localStorage.getItem(THEME_STORAGE_KEY)
    return isThemePreference(storedPreference) ? storedPreference : 'system'
  } catch {
    return 'system'
  }
}

export const useThemeStore = defineStore('theme', () => {
  const preference = ref<ThemePreference>('system')
  const systemTheme = ref<ResolvedTheme>('light')
  let mediaQuery: MediaQueryList | undefined

  const resolvedTheme = computed<ResolvedTheme>(() =>
    preference.value === 'system' ? systemTheme.value : preference.value,
  )

  function applyTheme(): void {
    if (typeof document === 'undefined') return
    document.documentElement.dataset.theme = resolvedTheme.value
  }

  function updateSystemTheme(event?: MediaQueryListEvent): void {
    const prefersDark = event?.matches ?? mediaQuery?.matches ?? false
    systemTheme.value = prefersDark ? 'dark' : 'light'

    if (preference.value === 'system') applyTheme()
  }

  function persistPreference(): void {
    if (typeof window === 'undefined') return

    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, preference.value)
    } catch {
      // A preferência continua válida durante a sessão quando o storage está indisponível.
    }
  }

  function setTheme(nextPreference: ThemePreference): void {
    preference.value = nextPreference
    persistPreference()
    applyTheme()
  }

  function initialize(): void {
    preference.value = readStoredPreference()

    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      mediaQuery?.removeEventListener('change', updateSystemTheme)
      mediaQuery = window.matchMedia(DARK_THEME_QUERY)
      mediaQuery.addEventListener('change', updateSystemTheme)
    }

    updateSystemTheme()
    applyTheme()
  }

  function dispose(): void {
    mediaQuery?.removeEventListener('change', updateSystemTheme)
    mediaQuery = undefined
  }

  return {
    preference,
    resolvedTheme,
    initialize,
    setTheme,
    dispose,
  }
})
