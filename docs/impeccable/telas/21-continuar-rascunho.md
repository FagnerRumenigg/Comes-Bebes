# Tela 21 — Continuar rascunho

Referência: `frontend/src/views/CreatePublicationView.vue` (`/publicar/rascunho/:draftId`)

## Status

- Critique: concluído
- P0 — recuperação sem perda: **corrigido na branch `correcoes-p0`**
- Visual: aguardando próxima etapa
- Texto, cor e identidade: etapa posterior

## Principais pontos

- **[P0] Recuperar o trabalho:** abrir o rascunho no mesmo estado em que foi salvo, sem surpresa ou perda.
- **[P1] Contexto:** informar que a pessoa está continuando um rascunho e mostrar última atualização.
- **[P1] Ações:** salvar, publicar, cancelar e voltar precisam ter consequências claras.
- **[P2] Estado inválido:** tratar rascunho removido, expirado ou inacessível com caminho de recuperação.
- **[P2] Detector:** nenhum alerta automatizado no componente.

## Correção realizada

- Autosaves concorrentes agora são serializados e o snapshot mais recente é enfileirado.
- Falhas do IndexedDB deixam de ser silenciosas e aparecem para a pessoa.
- Um ID de rascunho inexistente apresenta estado próprio com caminhos de recuperação.
- Não foi necessária alteração no backend ou contrato de API.

## Validação

- `npm run typecheck`: passou.
- Testes direcionados de rascunhos, publicação e minha versão: 17 passaram.
- `npm run lint`: 0 erros; 4 avisos preexistentes fora do escopo.
