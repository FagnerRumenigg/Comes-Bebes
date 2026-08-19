import { onMounted, onUnmounted } from 'vue'

import { httpClient } from '@/api/client'

/**
 * Teste: ping periódico pro backend, só pra ver se mantém o Container App
 * vivo (cooldown de scale-to-zero é 300s/5min - 4min de intervalo fica
 * dentro da janela). Só evita o cold start enquanto uma aba estiver aberta;
 * não substitui a modal/tela de aviso pros casos em que ninguém está com o
 * site aberto.
 */
const PING_INTERVAL_MS = 4 * 60 * 1000

export function useKeepAlivePing(): void {
  let timer: ReturnType<typeof setInterval> | undefined

  onMounted(() => {
    timer = setInterval(() => {
      void httpClient.get('/actuator/health/liveness').catch(() => {
        // Best-effort - se falhar, os mecanismos normais (modal/tela de
        // offline) já cobrem o aviso ao usuário.
      })
    }, PING_INTERVAL_MS)
  })

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })
}
