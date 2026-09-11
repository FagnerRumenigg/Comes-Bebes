// O interceptor XHR do MSW cria ProgressEvent para simular falhas de rede.
// O jsdom usado pelo Vitest não expõe essa classe em todas as versões.
if (typeof globalThis.ProgressEvent === 'undefined') {
  class TestProgressEvent extends Event {
    readonly lengthComputable = false
    readonly loaded = 0
    readonly total = 0
  }

  globalThis.ProgressEvent = TestProgressEvent as typeof ProgressEvent
}
