import '@fontsource/inter/latin-400.css'
import '@fontsource/inter/latin-500.css'
import '@fontsource/inter/latin-600.css'
import '@fontsource/inter/latin-700.css'
import '@fontsource/libre-caslon-text/latin-400.css'
import '@fontsource/libre-caslon-text/latin-400-italic.css'
import '@fontsource/libre-caslon-text/latin-700.css'

import { createApp, watch } from 'vue'

import App from '@/App.vue'
import { setAccessTokenProvider, setUnauthorizedHandler } from '@/api/client'
import { installApplicationPlugins, pinia, queryClient } from '@/app/plugins'
import { setRouteSessionResolver } from '@/app/router/guards'
import { useAuthStore } from '@/stores/auth.store'
import { useThemeStore } from '@/stores/theme.store'
import '@/styles/global.css'

async function bootstrap(): Promise<void> {
  const mocksEnabled = import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS !== 'false'
  if (mocksEnabled) {
    const { startMockServer } = await import('@/mocks/browser')
    await startMockServer()
  }

  const app = createApp(App)
  installApplicationPlugins(app)

  const authStore = useAuthStore(pinia)
  let activeUserId: string | null = null
  watch(
    () => authStore.identity?.userId ?? null,
    (userId) => {
      if (userId === activeUserId) return
      activeUserId = userId
      queryClient.clear()
    },
    { flush: 'sync' },
  )
  setAccessTokenProvider(() => authStore.accessToken)
  setUnauthorizedHandler(() => authStore.renewSession())
  setRouteSessionResolver(async () => {
    await authStore.initialize()
    return {
      authenticated: authStore.authenticated,
      role: authStore.identity?.role ?? null,
    }
  })

  useThemeStore(pinia).initialize()
  await authStore.initialize()
  app.mount('#app')
}

void bootstrap()
