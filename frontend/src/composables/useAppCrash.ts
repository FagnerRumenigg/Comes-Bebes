import { ref } from 'vue'

// Rede de segurança para exceções não tratadas durante render/setup (o Vue
// não consegue mais garantir que a árvore de componentes está íntegra) -
// mostra o fallback genérico (docs/telas/01-boas-vindas-e-erro.html #error)
// em vez de deixar a tela em branco. Ver app.config.errorHandler em main.ts.
export const appCrashed = ref(false)

export function reportAppCrash(error: unknown, info: string): void {
  console.error('Erro não tratado na aplicação:', error, info)
  appCrashed.value = true
}
