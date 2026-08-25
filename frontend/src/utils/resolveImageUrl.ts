const backendBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8082'

/**
 * As imagens do backend vêm como caminho relativo (ex.: "/images/images/uuid.webp").
 * Em produção front e back dividem a mesma origem, mas em dev local cada um roda
 * numa porta diferente — um <img src="/images/..."> ou `url(/images/...)` cru
 * pediria pro Vite (5173), não pro backend (8082), e a imagem quebra.
 */
export function resolveImageUrl(path: string): string {
  if (
    /^(?:https?:\/\/|data:|blob:)/i.test(path) ||
    path.startsWith('/src/') ||
    path.startsWith('/assets/')
  ) {
    return path
  }
  const joinedPath = `${backendBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
  return new URL(joinedPath, window.location.origin).toString()
}
