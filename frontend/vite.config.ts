import { execSync } from 'node:child_process'
import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

function resolveCommitHash(): string {
  try {
    return execSync('git rev-parse --short HEAD').toString().trim()
  } catch {
    return 'dev'
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_API_PROXY_TARGET
  const base = env.VITE_APP_BASE_PATH || '/'

  return {
    base,
    plugins: [vue()],
    define: {
      __APP_COMMIT_HASH__: JSON.stringify(resolveCommitHash()),
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: proxyTarget
      ? {
          proxy: {
            '/comesebebes': {
              target: proxyTarget,
              changeOrigin: true,
            },
          },
        }
      : undefined,
  }
})
