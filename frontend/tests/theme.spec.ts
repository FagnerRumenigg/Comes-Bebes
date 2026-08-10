import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { THEME_STORAGE_KEY, useThemeStore, type ResolvedTheme } from '@/stores/theme.store'

function installMatchMedia(initialTheme: ResolvedTheme = 'light') {
  let matches = initialTheme === 'dark'
  const listeners = new Set<(event: MediaQueryListEvent) => void>()

  const mediaQuery = {
    get matches() {
      return matches
    },
    media: '(prefers-color-scheme: dark)',
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn((_type: string, listener: EventListenerOrEventListenerObject) => {
      if (typeof listener === 'function') {
        listeners.add(listener as (event: MediaQueryListEvent) => void)
      }
    }),
    removeEventListener: vi.fn((_type: string, listener: EventListenerOrEventListenerObject) => {
      if (typeof listener === 'function') {
        listeners.delete(listener as (event: MediaQueryListEvent) => void)
      }
    }),
    dispatchEvent: vi.fn(),
  } as MediaQueryList

  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => mediaQuery),
  )

  return {
    setSystemTheme(theme: ResolvedTheme) {
      matches = theme === 'dark'
      const event = { matches } as MediaQueryListEvent
      listeners.forEach((listener) => listener(event))
    },
  }
}

describe('preferência de tema', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.localStorage.clear()
    delete document.documentElement.dataset.theme
  })

  it('restaura e aplica uma preferência persistida', () => {
    installMatchMedia('light')
    window.localStorage.setItem(THEME_STORAGE_KEY, 'dark')
    const themeStore = useThemeStore()

    themeStore.initialize()

    expect(themeStore.preference).toBe('dark')
    expect(themeStore.resolvedTheme).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('persiste alterações explícitas de tema', () => {
    installMatchMedia()
    const themeStore = useThemeStore()
    themeStore.initialize()

    themeStore.setTheme('dark')

    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('acompanha o sistema enquanto a preferência for automática', () => {
    const media = installMatchMedia('light')
    const themeStore = useThemeStore()
    themeStore.initialize()

    media.setSystemTheme('dark')

    expect(themeStore.preference).toBe('system')
    expect(themeStore.resolvedTheme).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })
})
