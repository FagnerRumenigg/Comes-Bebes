# Tela 20 — Rascunhos

Referência: `frontend/src/views/DraftsView.vue`

## Status

- Critique: concluído
- P0 — informar salvamento: **corrigido na branch `correcoes-p0`**
- Visual: aguardando próxima etapa
- Texto, cor e identidade: etapa posterior

## Principais pontos

- **[P0] Segurança do trabalho:** deixar explícito quando e como o rascunho foi salvo.
- **[P1] Retomar sem dúvida:** mostrar título, foto, tipo e última atualização para a pessoa escolher rapidamente.
- **[P1] Ações destrutivas:** apagar rascunho exige confirmação e possibilidade de recuperação quando viável.
- **[P1] Estado vazio:** incentivar registrar uma ideia ou voltar a explorar, sem pressionar a publicar.
- **[P2] Memória e processo:** tratar rascunhos como parte do caderno culinário, não como lixo temporário.
- **[P2] Detector:** nenhum alerta automatizado no componente.

## Correção realizada

- A tela já informava data e hora; o texto agora identifica explicitamente o último salvamento neste dispositivo.
- Falhas ao ler o IndexedDB agora exibem estado de erro e ação para tentar novamente.
- Não foi necessária alteração no backend.

## Validação

- Typecheck e testes direcionados devem ser executados após a implementação.
