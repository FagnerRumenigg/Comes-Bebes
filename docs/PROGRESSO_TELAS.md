# Progresso das telas

Checklist de validação de cada tela do pacote de design contra o que está
implementado no projeto. Ver `REGRAS_AGENTE.md` para o procedimento de
validação.

| # | Tela | Status |
|---|---|---|
| 00 | Marca | ✅ Concluída |
| 01 | Boas-vindas e erro | ✅ Concluída |
| 02 | Login e cadastro | ✅ Concluída |
| 03 | Carregando | ✅ Concluída |
| 04 | Navegação | ✅ Concluída |
| 05 | Feed | ✅ Concluída |
| 06 | Salvos | ✅ Concluída |
| 07 | Coleção | ✅ Concluída |
| 08 | Perfil | ✅ Concluída |
| 09 | Configurações | ✅ Concluída |
| 10 | Publicar | ✅ Concluída |
| 11 | Recuperar senha | ✅ Concluída |
| 12 | Avisos | ✅ Concluída |
| 13 | Minha versão | ⬜ Pendente |
| 14 | Editar perfil | ⬜ Pendente |

## Pendências deixadas de propósito

- **02 e 11 (login/recuperar senha):** resolvido — fluxo completo de
  "Esqueci minha senha" construído do zero (não existia nada antes: nem
  tabela, nem endpoint, nem envio de e-mail em lugar nenhum do projeto).
  Isso também foi a primeira vez que a infra de e-mail entrou de verdade
  (não só planejada) — ver nota de e-mail logo abaixo.
- **02 (login e cadastro):** falta "Entrar/Criar conta com o Google" — não
  existe login OAuth no backend, só JWT interno. Fica pra quando (e se) essa
  integração for feita no backend.
- **04 e 09 (navegação/configurações):** resolvido — item "Ajuda" adicionado
  ao menu da conta (`AccountMenuContent.vue`, leva a Configurações → Ajuda e
  sobre) e as 4 linhas que faltavam no pane "Ajuda e sobre" foram criadas:
  "Como usar o Comes&Bebes" (FAQ), "Falar com a gente" (tela de sugestão
  nova, `/sugestao`), "Termos de Serviço" e "Política de Privacidade"
  (`/termos`, `/privacidade`). Os links do rodapé de login/cadastro
  (`AuthLayout.vue`) também deixaram de ser `href="#"` e apontam pras
  mesmas rotas. Infra nova:
  - Tabela `content_documents` (migration `V35`) guarda Termos, Privacidade
    e FAQ — mesmo padrão de `patch_notes` (linhas mantidas direto no banco,
    sem endpoint de escrita). **O texto de todos os 3 é rascunho meu**,
    ainda não revisado juridicamente nem pelo dono do produto — precisa ser
    substituído antes de valer pra valer. `GET /documents/{slug}` é
    público (Termos/Privacidade precisam ser lidos sem estar logado).
  - Tabela `feedback_submissions` (migration `V35`) guarda o que a pessoa
    escreve em "Falar com a gente" (`POST /feedback`, autenticado). Por
    decisão explícita: só armazena, sem tela de admin nem e-mail de aviso
    por enquanto — ninguém lê essas mensagens ainda além de quem consultar
    o banco direto.
- **04 (navegação):** "Buscar" saiu do header — resolvido na tela 05, a busca
  já estava embutida no Feed (`FeedView.vue`), só faltava tirar o link
  duplicado do header.
- **05 (feed):** empty states ("A cozinha está quieta...") têm texto
  diferente do mockup ("A mesa ainda está posta..."), mas mantêm a mesma
  estrutura (o que aconteceu + como mudar + saída). Não mexido, prioridade
  baixa.
- **06 (salvos):** resolvido — `CollectionResponse` agora traz `coverImageUrls`
  (até 3 fotos das publicações da coleção, mesma convenção de URL do
  `PublicationResponseFactory`), e `CollectionCard.vue` mostra o mosaico
  igual à referência, com fallback pro ícone quando a coleção está vazia.
- **07 (coleção):** resolvido — cabeçalho da coleção também ganhou o mosaico
  de capa, e coleções "Para quem eu escolher" agora aceitam convite direto
  por @usuário (`PUT /collections/{id}/invitees`) e remover uma pessoa
  específica sem afetar as outras nem revogar o link (`DELETE
  /collections/{id}/invitees/{userId}`) — o link continua existindo como
  alternativa, não foi removido.
- **08 (perfil):** resolvido — três achados da própria legenda da tela 08:
  1) `users.bio` é campo novo (migration `V31`), com edição em
     `EditProfileView.vue` (tela 14 herda esse campo pronto quando chegarmos
     lá) e exibição em `ProfileView.vue` (com convite pra preencher, no
     próprio perfil, quando está vazio);
  2) removido o link solto "Meus dispositivos" do perfil — já mora em
     Configurações → Entrar e aparelhos, exatamente como a legenda da tela
     08 documentava ("foi para Configurações"), só não tinha sido tirado de
     lá;
  3) criada a página "Quem eu sigo" (`FollowingListView.vue`, rota
     `/u/:username/seguindo`), que não existia — o backend já tinha o
     endpoint (`GET /users/{id}/following`) pronto, só faltava a tela.
- **09 e 10 (configurações/publicar):** resolvido — `users.default_publication_visibility`
  é campo novo (migration `V32`, default `PUBLIC`), exposto só via `/auth/info`
  (é preferência privada, não faz parte do `UserResponse` público, mesma
  lógica de `notifyOnFollowedPublish`). Seção "Quem pode ver o que você
  publica" nova em Configurações → Minha conta, e `CreatePublicationView.vue`
  pré-seleciona a visibilidade com esse valor (sem sobrescrever se a pessoa
  já mexeu no campo ou está retomando um rascunho).
- **10 (publicar):** o título da publicação (`Como se chama?`) é opcional
  tanto no front quanto no back (`@Size` sem `@NotBlank`/`@NotNull`) — a
  referência tem um estado de erro "sem título" que hoje não existe mais.
  Como os dois lados concordam de forma consistente, tratei como evolução
  deliberada do produto (título deixou de ser obrigatório), não como bug.
  Só registrando caso não fosse essa a intenção.
- **10 (publicar):** a faixa "confirme seu e-mail" da referência (bloqueia
  publicar até confirmar e-mail, com "Reenviar e-mail"/"Trocar e-mail") ainda
  não existe — o projeto só tem o conceito de "conta sem e-mail" (contas
  antigas, migração), não "e-mail não confirmado" (precisaria de uma coluna
  tipo `email_confirmed_at`, token de confirmação e o gate no publicar). A
  infra de e-mail pra mandar esse link **já existe** agora (construída na
  tela 11 — `EmailSender`), então isso ficou mais barato de fazer depois; só
  não implementei sem confirmar o escopo.
- **11 (recuperar senha):** infra de e-mail construída (`EmailSender` +
  `AzureEmailConfig`, dependência `com.azure:azure-communication-email`),
  reaproveitando o padrão do Entertain-Me — modo `log` por padrão local (só
  loga, sem exigir credencial), `azure` em produção via
  `COMESEBEBES_EMAIL_DELIVERY_MODE`. Endpoint/chave reais do Azure Communication
  Services ainda faltam ser configurados quando formos pra produção — só
  variáveis vazias por enquanto (`infra/.env.example`). Tabela
  `password_reset_tokens` (migration `V33`, expira em 1h, uso único);
  `POST /auth/password-reset` sempre responde sucesso (não revela quem tem
  conta) e `POST /auth/password-reset/confirm` troca a senha e desconecta
  todos os aparelhos (`AuthService.logoutAll`). Tela nova
  `ForgotPasswordView.vue` com os 5 estados, rota
  `/recuperar-senha/:token?`.
- **09 e 12 (configurações/avisos):** resolvido — Fase A + Fase B do plano
  aprovado, feitas juntas:
  - Fase A: `users` ganhou 6 colunas novas de preferência (migration `V34`)
    além de `notify_on_followed_publish` (que já existia) — `notifyOnSaved`,
    `notifyOnReacted`, `notifyOnMyVersion`, `notifyOnCollectionNewItem`,
    `notifyOnCollectionShared`, `notifyWeeklyEmail`. Nova seção "Avisos" em
    Configurações (`SettingsNotificationsPane.vue`), substituindo o toggle
    solto e mal posicionado que existia em `DevicesView.vue` (removido).
    `notifyOnFollowedPublish` também mudou de default: `false` agora (a
    referência mostra esse item desligado por padrão), corrigido tanto no
    lado Java (`@Builder.Default`, o que realmente importa pro INSERT do
    Hibernate) quanto no SQL, só pra quem se cadastrar daqui pra frente.
  - Fase B: os 5 eventos que não existiam agora geram aviso — guardar
    publicação (`SavedPublicationService`), reagir (`ReactionService`),
    fazer "minha versão" (`PublicationService`), item novo numa coleção
    seguida e coleção compartilhada por @usuário (`CollectionService`) — cada
    um checando a preferência correspondente e nunca notificando a própria
    pessoa. `user_notifications` ganhou `collection_id` (migration `V34`) pra
    avisos de coleção.
  - "Parar de avisar sobre esta coleção" (menu de cada aviso) reaproveita o
    unfollow de coleção que já existia — não foi criado um sistema de mute
    separado.
  - **Simplificação combinada com o usuário:** a referência agrega vários
    atores num aviso só ("Ana e mais gente guardaram..."); implementado sem
    essa agregação — um aviso por evento, sempre com um único ator. Também
    não guardamos qual reação foi usada nem em qual coleção uma publicação
    foi guardada (o modelo de dados não amarra isso), então os textos de
    "reagiu" e "guardou" são genéricos, sem citar a reação ou a coleção de
    destino.
  - Fase C (resumo semanal por e-mail) **não entrou** — a infra de e-mail já
    existe (tela 11), falta o job `@Scheduled` e o template. Fica pendente.
- **Client gerado (orval) desatualizado:** `GET /auth/info` (tela 04, agora
  também com `defaultPublicationVisibility`), os parâmetros `scope`/`sort` de
  `GET /publications/feed` (tela 05), o campo `coverImageUrls` de
  `CollectionResponse` (telas 06/07), os endpoints `PUT`/`DELETE
  /collections/{id}/invitees...` (tela 07), os campos `bio` e
  `defaultPublicationVisibility` de `UpdateUserRequest` (telas 08/09/10),
  `POST /auth/password-reset` + `POST /auth/password-reset/confirm` (tela 11)
  e, da tela 12: os campos novos de `GET /users/{id}/notifications`
  (`collectionId`, `actorDisplayName`, `publicationTitle`,
  `publicationImageUrl`, `collectionName`, `createdAt`), os endpoints
  `PATCH /users/{id}/notifications/read`, `DELETE
  /users/{id}/notifications` e `DELETE /users/{id}/notifications/{id}`, e os
  6 campos novos de `GET`/`PATCH /users/{id}/notification-preferences` —
  existem no backend mas o `openapi.json`/client do frontend ainda não sabem
  disso — isso só atualiza rodando o backend e depois `npm run api:generate`.
  E da tela de Ajuda: `GET /documents/{slug}` e `POST /feedback` são
  endpoints inteiramente novos, ainda não existem no client gerado.
  Enquanto isso, `useAccountInfo.ts`, `feed.queries.ts`, `CollectionCard.vue`,
  `CollectionDetailsView.vue`, `ProfileView.vue`, `EditProfileView.vue`,
  `SettingsAccountPane.vue`, `CreatePublicationView.vue`,
  `ForgotPasswordView.vue`, `features/notifications/notifications.ts`,
  `features/settings/notificationPreferences.ts`,
  `features/documents/documents.ts`, `features/feedback/feedback.ts` e
  `NotificationsView.vue` tratam esses campos/rotas com tipos estendidos ou
  chamadas diretas na mão.
  Trocar pelos hooks/tipos gerados quando der. De quebra, o mock MSW de
  `GET /users/:id/notifications` (`mocks/handlers/discovery.ts`) ficou ainda
  mais desatualizado — já usava tipos (`PUBLICATION_APPROVED`,
  `PUBLICATION_REPORTED`) que nem existem no `CHECK` do banco; não foi
  corrigido agora porque exigiria também mockar os endpoints novos de
  apagar/limpar/marcar lido, fora do escopo desta tela.
