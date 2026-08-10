import { storeToRefs } from 'pinia'

import { useThemeStore } from '@/stores/theme.store'

export function useTheme() {
  const themeStore = useThemeStore()
  const { preference, resolvedTheme } = storeToRefs(themeStore)

  return {
    preference,
    resolvedTheme,
    setTheme: themeStore.setTheme,
  }
}
