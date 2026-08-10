# Comes&Bebes — Plano de Implementação do Front-end

## 1. Controle do documento

| Campo            | Valor                                                                            |
| ---------------- | -------------------------------------------------------------------------------- |
| Status           | Planejamento ativo                                                               |
| Versão           | 1.0                                                                              |
| Data inicial     | 08/08/2026                                                                       |
| Raiz do frontend | `ComesEBebes/`                                                                   |
| Progresso geral  | Fases 11.3 a 11.6 implementadas; validação real parcial com bloqueios no backend |

Este documento acompanha a execução do frontend. Ele não substitui
`FRONTEND-ARCHITECTURE.md`, `DESIGN-HANDOFF.md` nem o OpenAPI.

## 2. Fontes de verdade

1. `openapi/openapi.json`.
2. Regras de negócio e modelagem do backend.
3. `docs/FRONTEND-ARCHITECTURE.md`.
4. `docs/DESIGN-HANDOFF.md` e imagens em `docs/design/`.

Em caso de divergência, não criar DTOs manuais ou adaptações para esconder um contrato
incorreto. Corrigir o backend/OpenAPI e regenerar o cliente.

## 3. Estado validado em 08/08/2026

- OpenAPI 3.1 estruturalmente válido.
- 23 paths, 30 operações e 28 schemas declarados.
- 76 referências internas resolvidas.
- Nenhum `operationId` duplicado.
- Paginação corrigida para iniciar em 1, com tamanho entre 1 e 50.
- Geração integral pelo Orval validada e aplicada ao projeto.
- Resultado da geração integral: 85 arquivos e 74 arquivos de modelos/tipos derivados.
- Os grupos gerados são `authentication`, `moderation`, `profiles`, `publications` e `users`.
- O cliente versionado foi regenerado integralmente, sem transformer provisório.

## 4. Regras de acompanhamento

- Executar somente uma fase funcional por vez.
- Não iniciar uma fase se ela depender de uma pendência bloqueante da fase anterior.
- Atualizar os checkboxes e o histórico deste documento ao concluir cada fase.
- Cada fase termina com typecheck, lint, testes relevantes e build.
- Telas terminam também com comparação visual clara e escura em 1440 px.
- Mobile só poderá ser declarado fiel quando existirem referências em 390 px.
- Código em `src/api/generated` nunca será editado manualmente.

## 5. Fase 0 — Fechamento do contrato e atualização da geração

### Pendências bloqueantes do OpenAPI

- [x] Corrigir o acesso público ou opcional de `GET /publications/feed`.
- [x] Corrigir o acesso público ou opcional de `GET /publications/search`.
- [x] Corrigir o acesso público ou opcional de `GET /publications/{id}`.
- [x] Corrigir o acesso público ou opcional de `GET /publications/{id}/recipe`.
- [x] Tornar `GET /u/{username}` público.
- [x] Permitir que as publicações de um perfil público sejam consultadas sem Bearer obrigatório.
- [x] Revisar `POST /auth/refresh`: o refresh token deve funcionar mesmo quando o access token
      estiver expirado.
- [x] Remover IDs de usuário enviados pelo cliente em operações autenticadas e obter a identidade
      pelo token: `userId`, `reporterId`, `authorId`, `reviewerId` e `administratorId`.
- [x] Adicionar a `PublicationResponse` uma indicação explícita de visibilidade dos totais de
      reações, por exemplo `showReactionCounts`.
- [x] Definir como o feed receberá a prévia de receita necessária ao verso do `RecipeFlipCard`,
      evitando uma requisição adicional por card.
- [x] Incluir a referência de origem necessária a publicações `MY_VERSION`.
- [x] Incluir o estado `reportedByCurrentUser`, ou equivalente, para ocultar a ação de denúncia
      depois que ela já tiver sido realizada.
- [x] Tornar `NotificationResponse.readAt` anulável para notificações ainda não lidas.
- [x] Tornar `ModerationCaseResponse.reviewedBy`, `reviewedAt` e `decisionNote` anuláveis enquanto
      o caso estiver pendente.

### Melhorias recomendadas do contrato

- [x] Tipar `reactionCode`, itens de `selectedReactions` e chaves/totais de reação com os códigos
      `WOULD_EAT`, `WANT_TO_MAKE` e `COMFORT_FOOD`.
- [x] Definir o comportamento de busca quando `title` e `ingredient` forem omitidos.
- [x] Remover o formato JSON com `imageUrl` de `POST /publications` se o fluxo oficial aceitar
      exclusivamente upload multipart.
- [x] Alinhar a porta do OpenAPI (`8082`) com `VITE_API_BASE_URL` (`8082`) ou documentar a
      diferença por ambiente.
- [x] Substituir `operationId` genéricos como `find_1`, `find_2`, `delete_1` e `update_1` por nomes
      estáveis do domínio.

### Atualização do frontend após o contrato

- [x] Atualizar Node para a versão mínima declarada pelo projeto, 22.18 ou superior.
- [x] Remover `orval.input-transformer.js`.
- [x] Remover o transformer de `orval.config.ts`.
- [x] Executar `npm run api:generate` e versionar a geração completa.
- [x] Confirmar as 30 operações com `npm run api:check`.
- [x] Executar typecheck, lint, testes unitários, build e Playwright.

### Critério de conclusão

O cliente deve ser gerado integralmente a partir do OpenAPI, sem transformer, DTO manual ou
edição do código gerado. Todas as validações devem passar.

## 6. Fase 1 — Tokens, tipografia e temas

- [x] Criar reset global definitivo.
- [x] Extrair tokens semânticos das referências clara e escura.
- [x] Configurar Libre Caslon Text e Inter.
- [x] Criar tokens de espaçamento, tipografia, raio, sombra e transição.
- [x] Implementar tema claro e escuro com as mesmas estruturas de componentes.
- [x] Respeitar `prefers-reduced-motion`.
- [x] Criar testes mínimos para persistência e aplicação do tema.

### Critério de conclusão

Tokens centralizados, sem hexadecimais repetidos nos componentes, com temas funcionando e sem
implementar páginas definitivas.

## 7. Fase 2 — Componentes base

- [x] `BaseButton` e `BaseIconButton`.
- [x] `BaseInput`, `BaseTextarea`, `BaseSelect` e `BaseCheckbox`.
- [x] `BaseFieldError`.
- [x] `BaseDialog`.
- [x] `BaseToast`.
- [x] Estados de foco, erro, carregamento, selecionado e desabilitado.
- [x] Testes de montagem, teclado e acessibilidade dos componentes.

### Critério de conclusão

Primitivos reutilizáveis, acessíveis e testados, sem acoplamento a regras de negócio.

## 8. Fase 3 — Layouts, rotas e navegação

- [x] `AppLayout`, `AuthLayout` e `AdminLayout`.
- [x] `AppHeader`, `PageContainer`, `ThemeSwitch` e estrutura de navegação mobile.
- [x] Configurar todas as rotas documentadas.
- [x] Criar guards de visitante, usuário autenticado e administrador.
- [x] Implementar estados globais de carregamento e erro de navegação.

### Critério de conclusão

Layouts e rotas compartilhados, sem duplicação estrutural entre páginas.

## 9. Fase 4 — Cadastro, login e sessão

- [x] Implementar cadastro conforme o OpenAPI vigente.
- [x] Implementar login, refresh e logout.
- [x] Criar store Pinia exclusivamente para sessão e identidade atual.
- [x] Integrar o access token ao cliente HTTP central.
- [x] Preservar dados de formulário após erros recuperáveis.
- [x] Exibir erros gerais e por campo retornados pela API.
- [x] Testar cadastro, login, refresh, logout e guards.
- [x] Comparar cadastro e login em tema claro e escuro a 1440 px.

## 10. Fase 5 — Feed e PublicationCard

- [x] Implementar feed público e autenticado com Vue Query.
- [x] Implementar paginação iniciando em 1.
- [x] Criar `PublicationCard`, `PublicationHeader`, `PublicationImage` e `VisibilityBadge`.
- [x] Exibir autor por nome e username, sem avatar.
- [x] Tratar `PENDING_VALIDATION`, `UNDER_REVIEW`, vazio, erro e URL expirada.
- [x] Centralizar query keys e invalidações da feature.
- [x] Testar carregamento, erro, vazio e paginação.
- [x] Comparar o feed nos dois temas em 1440 px.

## 11. Fase 6 — Receita, reações e ações

- [x] Criar `RecipeFlipCard` com teclado, toque e movimento reduzido.
- [x] Criar `ReactionBar` com seleção e remoção independente.
- [x] Criar `SaveButton` com atualização otimista e rollback.
- [x] Implementar “Fiz também” sem incrementar a contagem antecipadamente.
- [x] Implementar denúncia e estado de denúncia já realizada.
- [x] Implementar detalhes completos da receita.
- [x] Criar `IngredientList`, `RecipeInstructions` e `OriginReference`.
- [x] Testar flip, reações, salvos, denúncia e navegação para detalhes.

## 12. Fase 7 — Criação de publicação

- [x] Criar seletor de `DISH`, `RECIPE` e `MY_VERSION`.
- [x] Implementar upload de exatamente uma imagem.
- [x] Validar JPEG, PNG ou WebP até 15 MB antes do envio.
- [x] Criar `IngredientEditor` com adicionar, remover e reordenar.
- [x] Implementar visibilidade `PUBLIC` e `INTERNAL`.
- [x] Preservar dados após erro e impedir submissão duplicada.
- [x] Implementar o fluxo `/publicar/minha-versao/:sourceId`.
- [x] Testar os três tipos de publicação e seus payloads gerados.

## 13. Fase 8 — Perfil, busca, salvos e notificações

- [x] Implementar perfil público por username.
- [x] Exibir somente nome, username e publicações.
- [x] Implementar busca por título ou ingrediente.
- [x] Implementar lista privada de salvos.
- [x] Implementar notificações e estado lido/não lido conforme o contrato final.
- [x] Testar paginação, vazio, erro, autorização e navegação.

## 14. Fase 9 — Moderação administrativa

- [x] Implementar fila de casos pendentes.
- [x] Implementar detalhe do caso por ID.
- [x] Implementar decisões `KEPT`, `HIDDEN` e `REMOVED`.
- [x] Exigir justificativa para ocultação e remoção.
- [x] Proteger rotas e ações para `ADMIN`.
- [x] Testar acesso negado, fila, detalhe e decisão.

## 15. Fase 10 — Responsividade, acessibilidade e acabamento

- [x] Validar todas as rotas em 1440 px nos temas claro e escuro.
- [x] Validar 390 px quando as referências mobile estiverem disponíveis.
- [x] Auditar navegação completa por teclado.
- [x] Auditar foco, labels, semântica, contraste e textos alternativos.
- [x] Auditar `prefers-reduced-motion`.
- [x] Executar toda a suíte E2E e visual.
- [x] Revisar bundle, cache, invalidações e estados de erro.
- [x] Confirmar que nenhuma funcionalidade fora do MVP foi adicionada.

## 16. Fase 11 — Integração com o backend real

### Estratégia

- Integrar uma fatia funcional por vez e validar antes de avançar.
- Manter os mocks para desenvolvimento isolado e testes rápidos.
- Usar `VITE_ENABLE_MOCKS=false` em todos os ambientes integrados.
- Adiar testes contra a API e o banco reais até todas as fases funcionais de integração estarem
  concluídas e houver uma nova decisão sobre essa suíte.
- Corrigir divergências no backend/OpenAPI e regenerar o cliente; nunca editar
  `src/api/generated` manualmente.

### Fase 11.0 — Ambiente real

- [x] Confirmar backend disponível em `http://localhost:8082`.
- [x] Confirmar OpenAPI real disponível em `GET /v3/api-docs`.
- [x] Confirmar acesso ao banco por meio do feed real e validar a carga de dados criada.
- [x] Confirmar preflight CORS para `http://localhost:5173`, incluindo `Authorization`,
      `Content-Type` e os métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`.
- [x] Criar `.env.local` com `VITE_API_BASE_URL=http://localhost:8082` e
      `VITE_ENABLE_MOCKS=false`.
- [x] Abrir o front em `http://localhost:5173` com mocks desligados e confirmar no navegador o
      feed real em `200`, o estado vazio e ausência de requisições com falha.

### Fase 11.1 — Contrato e cliente gerado

- [x] Garantir que o Playwright atual execute explicitamente com `VITE_ENABLE_MOCKS=true`, sem
      depender do `.env.local` usado na integração manual.
- [x] Obter o OpenAPI diretamente do backend em execução.
- [x] Comparar estruturalmente o contrato real com `openapi/openapi.json`.
- [x] Confirmar ausência de divergências: hashes idênticos, 23 paths, 30 operações e 28 schemas.
- [x] Regenerar o cliente com Orval e executar `npm run api:check`.
- [x] Executar typecheck, lint, testes unitários, build e Playwright sem adaptações manuais no
      código gerado.

### Fase 11.2 — Leitura pública

- [x] Validar feed vazio real, página iniciando em 1 e normalização do tamanho máximo para 50.
- [x] Validar busca real por título e o estado sem resultados; a tela não envia busca sem termo,
      que o backend rejeita com `SEARCH_TERM_REQUIRED`.
- [x] Validar perfil e publicação inexistentes com os códigos `USER_NOT_FOUND` e
      `PUBLICATION_NOT_FOUND`.
- [x] Evitar retry automático para erros 4xx, mantendo uma repetição para falhas de rede e 5xx.
- [x] Validar perfil real existente e suas publicações depois da integração de cadastro/login.
- [x] Validar cards, detalhes, receita, ordenação, `PUBLIC`, `INTERNAL` e URLs de imagem depois de
      existir ao menos uma publicação real no banco.

### Fase 11.3 — Autenticação e sessão

- [x] Conectar cadastro, login, refresh e logout ao cliente real e enviar o access token como
      `Bearer` somente a partir da memória.
- [x] Manter o refresh token no `sessionStorage` ou `localStorage`, conforme a opção de lembrar o
      dispositivo, e persistir sua rotação.
- [x] Padronizar tratamento de 400, 401, 403, 409 e 429.
- [x] Renovar a sessão depois de o navegador voltar ao primeiro plano ou uma requisição protegida
      receber 401, deduplicando chamadas simultâneas e evitando loops.
- [x] Alinhar o nome de exibição obrigatório ao comportamento real do backend, apesar de o OpenAPI
      atualmente permitir string vazia.
- [x] Validar no backend real, sem criar dados, `VALIDATION_ERROR`, `INVALID_CREDENTIALS` e
      `REFRESH_TOKEN_INVALID`.
- [x] Validar cadastro bem-sucedido, conflito de username, login, autorização Bearer,
      restauração da sessão, rotação e logout reais.
- [x] Confirmar que o refresh token anterior retorna `400 REFRESH_TOKEN_INVALID` depois da rotação.

### Fase 11.4 — Estado personalizado

- [x] Consumir `selectedReactions`, `saved` e `reportedByCurrentUser` conforme o usuário
      autenticado.
- [x] Implementar reagir/desfazer, salvar/remover, denúncia única e aba Salvos com estado
      otimista e reversão em caso de erro.
- [x] Limpar o cache personalizado na troca de identidade e invalidar todas as consultas de
      publicações afetadas.
- [x] Isolar por usuário e persistir após recarregar também no ambiente mockado.
- [x] Validar salvar, persistir, remover e restaurar o estado inicial contra o backend real.
- [x] Validar no backend real as três reações, remoção independente, persistência e bloqueio de
      reação na própria publicação.
- [x] Validar denúncia, estado `reportedByCurrentUser`, duplicidade `409 DUPLICATE_REPORT` e
      abertura do caso ao atingir três denunciantes.
- [x] Alinhar os motivos exibidos no `ReportDialog` com os seis códigos ativos do backend.

### Fase 11.5 — Criação e upload

- [x] Implementar multipart de prato, receita e `MY_VERSION` pelo cliente gerado.
- [x] Validar JPEG, PNG, WebP, limite de 15 MB, erros por campo e impedir submissão duplicada.
- [x] Liberar URLs temporárias de prévia e bloquear `MY_VERSION` quando a origem não existe.
- [x] Confirmar no ambiente mockado que o ID criado pode ser consultado imediatamente e após
      recarregar.
- [x] Alinhar a resposta mockada inicial a `PENDING_VALIDATION`.
- [ ] Revalidar na versão atual da API o multipart de prato, receita e `MY_VERSION`, consulta dos
      IDs, ingredientes, origem e conversão para WebP; a nova integração Java → validador bloqueia
      a conclusão.
- [x] Confirmar que uma PNG de 2,46 MB ultrapassa o limite multipart e chega ao validador, sem
      retornar `413`.
- [x] Corrigir no cliente central a parte `data` para `application/json`, eliminando o
      `415 Unsupported Media Type` sem editar o cliente gerado.
- [x] Alinhar frontend, testes, arquitetura, backend e OpenAPI no limite de 15 MB.
- [x] Sincronizar o OpenAPI servido pelo backend com `openapi/openapi.json`.
- [ ] Corrigir a integração Java → validador: o serviço Python aprova a imagem quando chamado
      diretamente, mas o backend retorna `INVALID_IMAGE` ou `IMAGE_VALIDATOR_UNAVAILABLE` para o
      mesmo arquivo.

### Fase 11.6 — Denúncia, notificações e moderação

- [x] Implementar denúncia única e impedir denúncia ou reação na própria publicação.
- [x] Exibir notificações e estados lido/não lido; o contrato atual não oferece operação para
      marcar como lida.
- [x] Proteger a moderação por papel `ADMIN` no roteador e tratar recusas do backend.
- [x] Implementar fila, detalhes, decisões, justificativa condicional e invalidação das
      publicações moderadas.
- [x] Validar autorização `ADMIN`, fila e detalhe de caso contra o backend real.
- [x] Validar as decisões `KEPT`, `HIDDEN` e `REMOVED` contra casos reais, incluindo a redução
      da fila, os estados finais `ACTIVE`, `HIDDEN` e `REMOVED` e a resolução dos relatórios.
- [x] Confirmar no banco a criação de `REPORT_REJECTED_WARNING` para o denunciante do caso
      mantido; `HIDDEN` e `REMOVED` não geram notificação pelo comportamento atual do serviço.
- [x] Validar pela API a leitura da notificação criada, incluindo o tipo
      `REPORT_REJECTED_WARNING` e o estado não lido.
- [x] Adicionar o texto específico de `REPORT_REJECTED_WARNING` na tela de notificações.

### Fase 11.7 — Testes com API real — adiada

- [x] Manter a suíte rápida atual com MSW.
- [x] Corrigir a resolução das imagens importadas pelos mocks, mantendo `/src/assets/...` e
      `/assets/...` na origem do frontend.
- [x] Atualizar o snapshot administrativo para incluir o botão de logout adicionado ao layout.
- [ ] Reavaliar a criação desta suíte somente após concluir a integração funcional.
- [ ] Se aprovada depois, criar configuração separada contra backend e banco descartável, com seed,
      usuários `USER`/`ADMIN`, limpeza de dados e origem CORS própria.

### Fase 11.8 — Homologação

- [ ] Configurar ambientes local, homologação e produção.
- [ ] Garantir HTTPS, CORS restrito e logs sem tokens.
- [ ] Revisar expiração da sessão e URLs de imagens.
- [ ] Executar toda a validação com mocks desligados.

## 17. Comandos de validação por fase

```powershell
npm run api:check
npm run typecheck
npm run lint
npm run format:check
npm run test:unit
npm run build
npm run test:e2e
npm audit
```

## 18. Histórico de progresso

| Data       | Fase         | Alteração                                                                                 | Resultado |
| ---------- | ------------ | ----------------------------------------------------------------------------------------- | --------- |
| 08/08/2026 | Fundação     | Vue, Router, Pinia, Vue Query, testes e ferramentas configurados                          | Concluído |
| 08/08/2026 | OpenAPI      | Paginação corrigida e geração completa validada temporariamente                           | Concluído |
| 08/08/2026 | Planejamento | Plano de execução e pendências restantes registrados                                      | Concluído |
| 08/08/2026 | Fase 0       | Contrato fechado, transformer removido e cliente integral gerado                          | Concluído |
| 08/08/2026 | Ambiente     | `fnm` reparado e Node 22.23.2 definido como padrão                                        | Concluído |
| 08/08/2026 | Fase 1       | Tokens, tipografia, temas e persistência implementados e testados                         | Concluído |
| 08/08/2026 | Fase 2       | Componentes base, estados e acessibilidade implementados e testados                       | Concluído |
| 08/08/2026 | Fase 3       | Layouts, rotas, guards e navegação responsiva implementados                               | Concluído |
| 08/08/2026 | Design       | Tema escuro atualizado para a paleta café, pêssego e verde                                | Concluído |
| 08/08/2026 | Fase 4       | Cadastro, login, sessão, mocks e snapshots visuais implementados                          | Concluído |
| 09/08/2026 | Fase 5       | Feed, cards, paginação, mocks e snapshots visuais implementados                           | Concluído |
| 09/08/2026 | Fase 6       | Receita, flip, reações, salvos, denúncia, detalhes e mocks implementados                  | Concluído |
| 09/08/2026 | Fase 7       | Criação de pratos, receitas, minha versão, upload, ingredientes e mocks implementados     | Concluído |
| 09/08/2026 | Fase 8       | Perfil público, busca, salvos, notificações e mocks implementados                         | Concluído |
| 09/08/2026 | Fase 9       | Fila, detalhe, decisões e proteção administrativa de moderação implementados              | Concluído |
| 09/08/2026 | Fase 10      | Auditoria visual, responsiva, acessibilidade, movimento reduzido e acabamento concluídos  | Concluído |
| 09/08/2026 | Fase 11.0    | Backend real, OpenAPI, banco, CORS e ambiente local verificados                           | Concluído |
| 09/08/2026 | Fase 11.1    | Contrato real comparado, cliente regenerado e E2E mockado isolado                         | Concluído |
| 09/08/2026 | Fase 11.2    | Feed, busca e erros reais validados; retry 4xx corrigido                                  | Parcial   |
| 09/08/2026 | Fase 11.3    | Sessão resiliente a expiração/401 e cadastro alinhado às validações reais                 | Parcial   |
| 09/08/2026 | Fase 11.4    | Estado personalizado isolado por conta, persistente e com cache coerente                  | Parcial   |
| 09/08/2026 | Fase 11.5    | Upload, validações, origem e consulta de publicação criada implementados                  | Parcial   |
| 09/08/2026 | Fase 11.6    | Denúncia, notificações, autorização e decisões administrativas implementadas              | Parcial   |
| 09/08/2026 | Teste real   | Autenticação, sessão, salvos e leitura administrativa validados; backend bloqueia escrita | Parcial   |
| 09/08/2026 | Reteste real | Upload e reações corrigidos na API; incompatibilidades restantes do frontend registradas  | Parcial   |
