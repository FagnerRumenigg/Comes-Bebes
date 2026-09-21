# Tela 10 — Publicar

Referência: `docs/telas/10-publicar.html`

## Status

- Critique: concluído
- Visual: aguardando demais telas
- Texto, cor e identidade: etapa posterior

## Principais pontos

- **[P0] Fluxo central:** publicar é a ação mais importante do produto; validar que foto, título, tipo e visibilidade formam uma sequência simples.
- **[P1] Estados de erro:** foto recusada, título ausente e e-mail não confirmado devem explicar o problema e preservar tudo que já foi preenchido.
- **[P1] Privacidade:** a escolha entre público, conta e privado precisa ser compreensível no momento da publicação.
- **[P1] Rascunho:** oferecer saída segura e deixar claro quando o conteúdo foi salvo.
- **[P2] “O que você fez?”:** é acolhedor, mas pode não cobrir descoberta, memória e registros de coisas que a pessoa comeu.
- **[P2] Detector:** padding apertado na área de foto, borda com sombra ampla e fontes comuns.

## Correção P0 relacionada

- O fluxo agora não inicia a publicação se o salvamento local final do rascunho falhar.
- O conteúdo permanece na tela para nova tentativa.
- Não houve alteração no contrato do backend.

## Pontos positivos

- Muitos estados importantes foram previstos.
- Há recuperação para foto recusada e e-mail não confirmado.
- O sucesso oferece ver no feed ou publicar outra coisa.
