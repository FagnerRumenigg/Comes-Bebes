# Tela 22 — Aceitar convite de coleção

Referência: `frontend/src/views/AcceptCollectionInviteView.vue`

## Status

- Critique: concluído
- P1: concluído e validado
- Convite, permissões e colaboração: aplicados

## Principais pontos

- **[P1] Explicar o convite:** a confirmação explica o acesso à coleção e o papel de colaborador antes do aceite.
- **[P1] Controle e privacidade:** o aceite é explícito, pode ser recusado sem penalidade e o login só é solicitado no momento necessário.
- **[P1] Token inválido:** mensagens específicas orientam convites expirados, já utilizados ou indisponíveis.
- **[P1] Colaboração:** o convite concede papel editor; colaboradores podem adicionar publicações, enquanto configurações, remoções e exclusão permanecem com o proprietário.
- **[P2] Mínimo de social:** enquadrar colaboração como curadoria de comida, não como relacionamento social.
- **[P2] Detector:** nenhum alerta automatizado no componente.
