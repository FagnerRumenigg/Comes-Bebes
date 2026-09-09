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
| 13 | Minha versão | ✅ Concluída |
| 14 | Editar perfil | ✅ Concluída |

## Pendências deixadas de propósito

- **Avatares de cozinha:** resolvido — a pendência dos dois `.docx` ("Avatares
  de cozinha (adiado) — por ora o avatar é a inicial do nome") foi fechada.
  Doze desenhos (panela, colher de pau, xícara, fatia de bolo, pão, tomate,
  milho, limão, garfo e faca, pimenta, ovo, abacaxi), na mesma linguagem
  visual de `marca.svg` — formas planas, só as cores de `tokens.css`,
  alternando os dois pares já validados na marca (verde/dourado e
  dourado/verde). Depois de testar num rascunho (Artifact), a pessoa pediu
  contorno mais forte pra separar as formas, pensando em acessibilidade (visão
  monocular) — o contorno usa `--color-text` (fica escuro no claro, claro no
  escuro) em vez de preto fixo, senão sumiria no tema escuro.
  - `users.avatar_key` novo (migration `V37`), string nula = continua na
    inicial. `UpdateUserRequest.avatarKey` aceita as 12 chaves ou string vazia
    pra limpar, mesmo padrão de `bio`. Exposto em `UserResponse` (perfil
    público) e em `UserInfoResponse`/`GET /auth/info` (pro menu da conta não
    precisar buscar o perfil público de novo).
  - Frontend: `kitchen-avatar-paths.ts` guarda os 12 desenhos,
    `KitchenAvatarIcon.vue` renderiza um deles, `BaseAvatar.vue` mostra o
    desenho escolhido em vez da inicial quando existe. Escolha do avatar fica
    em Editar perfil (tela 14) — onde a legenda da própria tela 08 já dizia
    que isso ia entrar. Aparece também no cabeçalho/menu da conta e no
    próprio Perfil.
  - **Espalhado depois pra todo lugar que já mostrava avatar**, a pedido do
    usuário: `authorAvatarKey` novo em `PublicationResponse` (autor no feed/
    card de publicação, via `PublicationResponseFactory`) e em
    `CollectionResponse` (autor da coleção, os dois call-sites de
    `CollectionResponse.of` já tinham o `User author` em mãos); `actorAvatarKey`
    novo em `NotificationResponse`/`NotificationResponseFactory` (quem
    originou o aviso). "Quem eu sigo" e a lista de convidados de uma coleção
    já reaproveitam `UserResponse` puro, então só precisaram do campo no
    template (`FollowingListView.vue`, `CollectionDetailsView.vue`). Sem
    coluna nova no banco — é sempre o mesmo `users.avatar_key`, só mais
    lugares lendo.

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
- **13 (minha versão):** resolvido — a tela já existia (`CreatePublicationView.vue`
  reaproveitado com `sourceId`, seção 21.6 de `REGRAS_AGENTE.md`), mas nunca tinha
  passado pela validação formal palavra por palavra. Achados corrigidos:
  1) o estado vazio da foto mostrava o texto genérico de Publicar ("Comece pela
     foto") mesmo no fluxo de minha versão, sem avisar que a foto tem que ser
     da própria pessoa — agora mostra "Mostre como o seu ficou" / "A foto tem
     que ser sua. A foto original continua na publicação de {autor}." só nesse
     fluxo;
  2) faltava a frase "Você pode trocar a foto à vontade agora." na nota de foto
     já escolhida (a referência tem essa frase tanto em `10-publicar.html`
     quanto em `13-minha-versao.html`; só a segunda frase estava implementada)
     — corrigido nos dois fluxos, já que é o mesmo trecho de template;
  3) faltava o texto explicando por que o título já vem com um prefixo fixo
     ("O nome de {autor} fica no começo...") — adicionado;
  4) os textos de ajuda de Ingredientes e Modo de preparo eram genéricos mesmo
     quando os campos já vinham preenchidos com a receita original — agora
     `IngredientEditor.vue`/`PreparationStepsEditor.vue` aceitam um `hint`
     opcional, e minha versão passa um texto que explica a origem do
     conteúdo pré-preenchido.
  Um ponto ficou só registrado, não mudado: a referência visual de ambas as
  telas mostra Ingredientes/Modo de preparo como texto livre (uma
  `<textarea>`), e `REGRAS_AGENTE.md` §6 documenta que campos repetíveis com
  botões de adicionar/remover/reordenar por ingrediente já foram tentados e
  substituídos por texto livre antes. O código atual (`IngredientEditor.vue`,
  `PreparationStepsEditor.vue`) usa exatamente esse padrão de campos
  repetíveis, e já está assim também em `10-publicar.html`/tela 10 (✅
  concluída antes desta sessão) — não é uma regressão desta tela, é
  consistente com o que já estava em produção. Provável motivo: o backend
  espera ingredientes estruturados (`CreateRecipeRequest.ingredients`, usado
  também na busca por ingrediente). Não mexido agora porque afeta as duas
  telas e é decisão de produto, não bug — só fica registrado que
  `REGRAS_AGENTE.md` e o código divergem nesse ponto específico, para alguém
  decidir e atualizar um dos dois.
- **14 (editar perfil):** resolvido — a tela já existia
  (`EditProfileView.vue`), primeira validação formal encontrou dois textos
  que não batiam com a referência: rótulo do campo de descrição era
  "Descrição" (referência: "Escreva alguma coisa sobre você") e o texto de
  ajuda dizia "Um pouco sobre você — aparece no seu perfil." em vez de
  "Aparece no seu perfil, embaixo do nome. Pode deixar em branco." — os dois
  corrigidos. Também faltava o contador de caracteres que a referência mostra
  junto do campo; adicionado (sem mudar `BaseTextarea.vue`, que é compartilhado
  por muitas outras telas — o contador é markup local desta tela). Não mudado:
  a referência usa `maxlength` de 40 (nome) e 20 (@usuário), o código usa 100
  e 30 — ambos abaixo do limite real do backend (100 pros dois,
  `UpdateUserRequest`), então não bloqueiam nada válido; fica como estava por
  não ter como saber se o número menor da referência era intencional.
- **Falar com a gente (`/sugestao`, `FeedbackView.vue`):** validado contra o
  checklist geral de `REGRAS_AGENTE.md` (não tem HTML de referência dedicado
  no pacote de telas) — sem vocabulário banido, cobre os cinco estados
  (vazio, preenchendo, carregando, erro, sucesso) e bate com
  `CreateFeedbackRequest` do backend. Nenhuma correção necessária.
- **Painel/aviso de "Falar com a gente":** resolvido — decisão tomada com o
  usuário: aviso dentro do próprio app (reaproveita o sistema de avisos da
  tela 12), não e-mail (a credencial real do Azure Communication Services pra
  produção ainda não está configurada — ver nota da tela 11 — então um aviso
  por e-mail não chegaria de verdade ainda); painel só de leitura, sem marcar
  como resolvido por enquanto.
  - `GET /feedback` (ADMIN, paginado, mais recente primeiro) — novo painel
    `/admin/feedback` (`FeedbackQueueView.vue`, link em `AdminLayout.vue`).
  - Tipo de aviso novo `NEW_FEEDBACK_RECEIVED` (migration `V36`, sem
    preferência de usuário — ao contrário dos outros 5 avisos da tela 12, este
    fica sempre ligado e só existe pra quem tem role ADMIN)
    (`FeedbackService.notifyAdmins`), exibido em `NotificationsView.vue` como
    qualquer outro aviso e levando pra `/admin/feedback`.
- **Client gerado (orval) — a causa raiz era outra:** o regenerado desta
  sessão (`npm run api:generate`) não mudava nada porque `orval.config.ts`
  lê de um arquivo estático (`frontend/openapi/openapi.json`), não do backend
  ao vivo — faltava o passo manual de antes buscar `GET /v3/api-docs` do
  backend rodando e sobrescrever esse arquivo. Feito agora (`curl
  localhost:8082/v3/api-docs -o openapi/openapi.json && npm run
  api:generate`), o client finalmente pegou tudo que estava pendente: `GET
  /auth/info` com `defaultPublicationVisibility`, `scope`/`sort` do feed,
  `coverImageUrls`, os endpoints de convite de coleção, `bio` e
  `defaultPublicationVisibility` de `UpdateUserRequest`, os dois endpoints de
  recuperação de senha, os campos novos de avisos/preferências de aviso, e
  os endpoints de Ajuda (`GET /documents/{slug}`, `POST`/`GET /feedback`).
  `npm run api:check` confirma consistência agora.
  Só `features/feedback/feedback.ts` foi migrado pros hooks gerados nesta
  sessão (arquivo removido, `FeedbackView.vue`/`FeedbackQueueView.vue` usam
  `@/api/generated/feedback/feedback` direto). Os demais hand-rolled
  continuam como estavam — `useAccountInfo.ts`, `feed.queries.ts`,
  `CollectionCard.vue`, `CollectionDetailsView.vue`, `ProfileView.vue`,
  `EditProfileView.vue`, `SettingsAccountPane.vue`, `CreatePublicationView.vue`,
  `ForgotPasswordView.vue`, `features/notifications/notifications.ts`,
  `features/settings/notificationPreferences.ts` e
  `features/documents/documents.ts` — os tipos deles já batem com o gerado
  (só não foram trocados pelos hooks), então não é mais uma pendência de
  correção, só de limpeza. Trocar quando der.
  De quebra, corrigidos pra tipar contra o client real: `mocks/handlers/collections.ts`
  e `mocks/handlers/discovery.ts` (faltava `coverImageUrls`/`bio` nas
  respostas mockadas) e os fixtures de `collection-details.spec.ts`,
  `profile.spec.ts`, `saved.spec.ts`. O mock MSW de
  `GET /users/:id/notifications` (`mocks/handlers/discovery.ts`) continua
  desatualizado — já usava tipos (`PUBLICATION_APPROVED`,
  `PUBLICATION_REPORTED`) que nem existem no `CHECK` do banco; não corrigido
  porque exigiria também mockar os endpoints novos de apagar/limpar/marcar
  lido, fora do escopo desta tela.
