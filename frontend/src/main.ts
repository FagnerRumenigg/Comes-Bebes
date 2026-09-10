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
import { reportAppCrash } from '@/composables/useAppCrash'
import { initializeOnlineStatus } from '@/composables/useOnlineStatus'
import { useAuthStore } from '@/stores/auth.store'
import { useReducedMotionStore } from '@/stores/reducedMotion.store'
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
  app.config.errorHandler = (error, _instance, info) => reportAppCrash(error, info)
  initializeOnlineStatus()

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
      onboardingCompleted: authStore.identity?.onboardingCompleted ?? true,
      emailRequired: authStore.identity?.emailRequired ?? false,
    }
  })

  useThemeStore(pinia).initialize()
  useReducedMotionStore(pinia).initialize()
  app.mount('#app')
  // Aviso estático de index.html: só cobre o instante antes da Vue montar
  // (ex.: o bundle ainda carregando). Não esperamos authStore.initialize()
  // pra montar - se esperássemos, um /auth/refresh que demora (backend
  // acordando de scale-to-zero) travaria a Vue inteira fora do DOM, e só o
  // aviso estático (sem a animação/jogo) apareceria. Montando logo, a
  // BackendOfflineScreen (dirigida por backendStatus, useBackendStatus.ts)
  // assume esse aviso assim que a requisição entrar em curso.
  document.getElementById('boot-notice')?.remove()
  void authStore.initialize()
}

void bootstrap()
