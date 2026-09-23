# Tela 16 — Busca

Referência: `frontend/src/views/FeedView.vue` (busca integrada ao Feed)

## Status

- Critique: concluído
- P1: concluído e validado
- Visual, texto e filtros: aplicados na busca integrada ao Feed

## Principais pontos

- **[P1] Busca como descoberta:** campo integrado ao Feed, com linguagem orientada a títulos e conteúdo culinário.
- **[P1] Estado sem resultado:** estado vazio orienta a simplificar a busca ou tentar outro termo.
- **[P1] Estado inicial:** o Feed permanece visível antes da primeira busca, preservando a descoberta contínua.
- **[P2] Filtros:** evitar duplicar a complexidade do feed; deixar refinamentos progressivos.
- **[P2] Acessibilidade:** garantir label, foco, leitura do resultado e feedback de carregamento.
- **[P2] Detector:** nenhum alerta automatizado no componente.
