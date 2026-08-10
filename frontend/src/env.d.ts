/// <reference types="vite/client" />

export {}

declare global {
  interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string
    readonly VITE_API_PROXY_TARGET?: string
    readonly VITE_ENABLE_MOCKS?: string
  }

  interface ImportMeta {
    readonly env: ImportMetaEnv
  }
}

declare module 'vue-router' {
  interface RouteMeta {
    access?: 'public' | 'guest' | 'authenticated' | 'admin'
    pageTitle?: string
  }
}
