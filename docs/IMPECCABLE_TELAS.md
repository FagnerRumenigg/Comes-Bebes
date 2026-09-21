# Plano de revisão das telas — Impeccable

Este arquivo organiza a evolução visual e de experiência do Comes&Bebes.

As análises detalhadas ficam em `docs/impeccable/telas/`, com um arquivo por tela. Este documento funciona como índice e acompanhamento geral.

## Ordem de trabalho

1. **Critique:** revisar UX, hierarquia, acessibilidade, responsividade, estados e coerência com o produto.
2. **Visual:** aplicar melhorias de layout, espaçamento, tipografia, componentes, cores secundárias e detalhes visuais.
3. **Texto, cor e identidade:** revisar microcopy, mensagens, contraste, paleta secundária e expressão da marca.
4. **Conclusão:** após implementar e validar uma alteração, marcar o status correspondente em azul neste arquivo.

Nenhuma tela deve avançar para a próxima etapa sem concluir a anterior.

Status em azul significa que a alteração foi implementada e validada.

## Telas do produto

| # | Tela | Rota | Implementação | Status |
|---|---|---|---|---|
| 00 | [Marca / identidade](impeccable/telas/00-marca.md) | — | `docs/telas/00-marca.html` | **Critique concluído** |
| 01 | [Boas-vindas e erro](impeccable/telas/01-boas-vindas-e-erro.md) | `/bem-vindo` | `WelcomeView.vue` | <span style="color:#2563eb"><strong>P1 corrigido</strong></span> |
| 02 | [Login](impeccable/telas/02-login.md) | `/login` | `LoginView.vue` + `AuthLayout.vue` | **Critique concluído** |
| 03 | [Carregando](impeccable/telas/03-carregando.md) | transversal | `docs/telas/03-carregando.html` | **Critique concluído** |
| 04 | [Navegação](impeccable/telas/04-navegacao.md) | transversal | `AppLayout.vue` | <span style="color:#2563eb"><strong>P1 corrigido</strong></span> |
| 05 | [Feed](impeccable/telas/05-feed.md) | `/` | `FeedView.vue` | **Critique concluído** |
| 06 | [Salvos](impeccable/telas/06-salvos.md) | `/salvos` | `SavedView.vue` | **Critique concluído** |
| 07 | [Coleção](impeccable/telas/07-colecao.md) | `/colecoes/:id` | `CollectionDetailsView.vue` | **Critique concluído** |
| 08 | [Perfil](impeccable/telas/08-perfil.md) | `/u/:username` | `ProfileView.vue` | **Critique concluído** |
| 09 | [Configurações](impeccable/telas/09-configuracoes.md) | `/configuracoes/:secao?` | `SettingsView.vue` | **Critique concluído** |
| 10 | [Publicar](impeccable/telas/10-publicar.md) | `/publicar` | `CreatePublicationView.vue` | **Critique concluído** |
| 11 | [Recuperar senha](impeccable/telas/11-recuperar-senha.md) | `/recuperar-senha/:token?` | `ForgotPasswordView.vue` | **Critique concluído** |
| 12 | [Avisos / notificações](impeccable/telas/12-avisos.md) | `/notificacoes` | `NotificationsView.vue` | **Critique concluído** |
| 13 | [Minha versão](impeccable/telas/13-minha-versao.md) | `/publicar/minha-versao/:sourceId` | `CreatePublicationView.vue` | **Critique concluído** |
| 14 | [Editar perfil](impeccable/telas/14-editar-perfil.md) | `/perfil/editar` | `EditProfileView.vue` | **Critique concluído** |
| 15 | [Cadastro](impeccable/telas/15-cadastro.md) | `/cadastro` | `RegisterView.vue` | <span style="color:#2563eb"><strong>P0 corrigido</strong></span> |
| 16 | [Busca](impeccable/telas/16-busca.md) | `/buscar` | `SearchView.vue` | **Critique concluído** |
| 17 | [Detalhes da publicação](impeccable/telas/17-detalhes-publicacao.md) | `/publicacoes/:id` | `PublicationDetailsView.vue` | **Critique concluído** |
| 18 | [Editar publicação](impeccable/telas/18-editar-publicacao.md) | `/publicacoes/:id/editar` | `EditPublicationView.vue` | **Critique concluído** |
| 19 | [Seguindo](impeccable/telas/19-seguindo.md) | `/u/:username/seguindo` | `FollowingListView.vue` | **Critique concluído** |
| 20 | [Rascunhos](impeccable/telas/20-rascunhos.md) | `/rascunhos` | `DraftsView.vue` | **Critique concluído** |
| 21 | [Continuar rascunho](impeccable/telas/21-continuar-rascunho.md) | `/publicar/rascunho/:draftId` | `CreatePublicationView.vue` | **Critique concluído** |
| 22 | [Aceitar convite de coleção](impeccable/telas/22-aceitar-convite-colecao.md) | `/colecoes/convite/:token` | `AcceptCollectionInviteView.vue` | **Critique concluído** |
| 23 | [Meus dispositivos](impeccable/telas/23-dispositivos.md) | `/dispositivos` | `DevicesView.vue` | <span style="color:#2563eb"><strong>P0 corrigido</strong></span> |
| 24 | [Informar e-mail](impeccable/telas/24-informar-email.md) | `/informar-email` | `RequireEmailView.vue` | **Critique concluído** |
| 25 | [Termos de Serviço](impeccable/telas/25-termos.md) | `/termos` | `DocumentView.vue` | **Critique concluído** |
| 26 | [Política de Privacidade](impeccable/telas/26-privacidade.md) | `/privacidade` | `DocumentView.vue` | <span style="color:#2563eb"><strong>P0 corrigido</strong></span> |
| 27 | [Ajuda / FAQ](impeccable/telas/27-ajuda-faq.md) | `/faq` | `DocumentView.vue` | **Critique concluído** |
| 28 | [Falar com a gente](impeccable/telas/28-falar-com-a-gente.md) | `/sugestao` | `FeedbackView.vue` | **Critique concluído** |
| 29 | [Página não encontrada](impeccable/telas/29-pagina-nao-encontrada.md) | qualquer rota inválida | `NotFoundView.vue` | **Critique concluído** |
| 30 | [Fila de moderação](impeccable/telas/30-fila-moderacao.md) | `/admin/moderacao` | `ModerationQueueView.vue` | <span style="color:#2563eb"><strong>P0 corrigido</strong></span> |
| 31 | [Análise de moderação](impeccable/telas/31-analise-moderacao.md) | `/admin/moderacao/:caseId` | `ModerationCaseView.vue` | <span style="color:#2563eb"><strong>P0 corrigido</strong></span> |
| 32 | [Fila de feedback administrativo](impeccable/telas/32-fila-feedback.md) | `/admin/feedback` | `FeedbackQueueView.vue` | **Critique concluído** |

## Correções prioritárias

As correções serão trabalhadas na branch `correcoes-p0`; antes de alterar comportamento, será verificado se a solução exige mudanças no backend, contratos da API, banco ou apenas no frontend.

### P0 — bloquear riscos críticos

- ~~Recuperar rascunho sem perda de dados — Tela 21: Continuar rascunho.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- ~~Informar claramente quando o rascunho foi salvo — Tela 20: Rascunhos.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- ~~Garantir publicação completa e previsível — Tela 10: Publicar.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- Criar minuta estruturada — <span style="color:#2563eb"><strong>aprovada pelo solicitante</strong></span>; processamento internacional identificado e data de nascimento implementada — Tela 26: Política de Privacidade.
- ~~Proteger identificação e encerramento de sessões — Tela 23: Meus dispositivos.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- ~~Exigir decisão fundamentada na moderação — Tela 31: Análise de moderação.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- ~~Priorizar e ordenar corretamente a fila de casos — Tela 30: Fila de moderação.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>

### P1 — corrigir impacto alto

- ~~Corrigir recorte de menus posicionados — Tela 04: Navegação.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- ~~Reduzir fricção e reforçar o propósito na entrada — Tela 01: Boas-vindas.~~ <span style="color:#2563eb"><strong>Concluído</strong></span>
- Conectar login à memória e ao registro culinário — Tela 02: Login.
- Tornar feed, filtros e estados vazios mais claros — Tela 05: Feed.
- Simplificar salvos e reforçar biblioteca pessoal — Tela 06: Salvos.
- Organizar ações de coleção por contexto e privacidade — Tela 07: Coleção.
- Priorizar comida sobre métricas sociais no perfil — Tela 08: Perfil.
- Tornar configurações e ações destrutivas mais seguras — Tela 09: Configurações.
- Preservar dados e explicar erros durante publicação — Tela 10: Publicar.
- Tornar recuperação de senha segura e compreensível — Tela 11: Recuperar senha.
- Agrupar avisos e tornar cada item acionável — Tela 12: Avisos.
- Preservar relação com o original ao criar versão — Tela 13: Minha versão.
- Corrigir contraste e segurança ao salvar perfil — Tela 14: Editar perfil.
- Reduzir fricção e melhorar validação do cadastro — Tela 15: Cadastro.
- Transformar busca em descoberta culinária — Tela 16: Busca.
- Priorizar conteúdo e ações na publicação — Tela 17: Detalhes da publicação.
- Preservar conteúdo durante edição — Tela 18: Editar publicação.
- Manter o mínimo de social na tela de seguindo — Tela 19: Seguindo.
- Explicar convite, permissões e erros do token — Tela 22: Convite de coleção.
- Tornar termos legíveis, localizáveis e atualizados — Tela 25: Termos.
- Oferecer recuperação útil na página inexistente — Tela 29: Página não encontrada.
- Separar tipos, status e contexto do feedback — Tela 32: Fila de feedback.

### P2 — melhorar experiência

- Refinar estados, ações secundárias, tipografia, espaçamento e cores secundárias — Telas 00 a 32, conforme arquivos individuais.

### P3 — acabamento

- Revisar sombras, bordas, fontes e detalhes de consistência visual — Telas 00 a 32, conforme arquivos individuais.
