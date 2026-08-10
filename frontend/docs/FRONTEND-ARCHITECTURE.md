# Comes&Bebes — Arquitetura do Front-end

## 1. Controle do documento

| Campo | Valor |
|---|---|
| Status | Arquitetura inicial aprovada para implementação |
| Versão | 1.0 |
| Data | 08/08/2026 |
| Front-end | Vue 3 + TypeScript |
| Backend | Java 25 + Spring Boot 4 + REST |
| Responsáveis | Fagner e ChatGPT |

## 2. Objetivo

Definir como o front-end do **Comes&Bebes** deverá ser criado, organizado e integrado à API existente. O objetivo é produzir uma aplicação simples de manter, com componentes reutilizáveis, tipagem forte e fidelidade aos designs aprovados.

Este documento não redefine regras de negócio nem contratos do backend. As fontes de verdade são, nesta ordem:

1. A especificação OpenAPI exposta pela API em execução.
2. `regras-de-negocio-rede-social-comida.md`.
3. `modelagem-banco-rede-social-comida.md`.
4. `DESIGN-HANDOFF.md` e as imagens exportadas do Visily.

Se houver divergência entre o OpenAPI e a implementação real do backend, corrigir a API ou sua especificação. Não criar tipos manuais no front para esconder a divergência.

## 3. Stack

### Obrigatória

- Vue 3.
- TypeScript em modo estrito.
- Vite.
- Vue Router.
- Pinia.
- TanStack Vue Query para estado remoto.
- Orval para gerar tipos, cliente HTTP e composables a partir do OpenAPI.
- Vitest e Vue Test Utils.
- Playwright para fluxos e validação visual.
- ESLint e Prettier.

### Diretrizes

- Usar Composition API e `<script setup lang="ts">`.
- Não usar Vuex.
- Não adotar uma biblioteca visual completa como Vuetify ou PrimeVue: seus estilos competiriam com a identidade editorial.
- Preferir CSS custom properties, CSS global para tokens e estilos `scoped` nos componentes.
- Bibliotecas headless poderão ser usadas apenas para comportamentos complexos de acessibilidade, como modal e menu, sem impor aparência.
- Não adicionar dependência quando Vue ou CSS resolvem o problema com clareza.

## 4. Distribuição de responsabilidades

| Camada | Responsabilidade |
|---|---|
| `views` | Representar rotas e compor features e componentes. |
| `layouts` | Estruturas compartilhadas de página, navegação e autenticação. |
| `components/base` | Primitivos visuais genéricos do design system. |
| `components/domain` | Componentes reutilizáveis próprios do Comes&Bebes. |
| `features` | Fluxos funcionais e componentes específicos de um caso de uso. |
| `api/generated` | Código gerado pelo OpenAPI; nunca editar manualmente. |
| `api/adapters` | Conversões puramente visuais quando o DTO não deve ser usado diretamente. |
| `stores` | Estado global do cliente, como sessão e tema. |
| Vue Query | Estado obtido do servidor, cache, carregamento, erro e invalidação. |
| `composables` | Lógica reutilizável que não representa estado global. |

### Regra para criar componentes

Criar um componente quando pelo menos uma destas condições for verdadeira:

1. É reutilizado em mais de uma tela.
2. Possui comportamento próprio.
3. Representa um conceito reconhecível do produto.
4. Isolá-lo melhora teste, acessibilidade ou legibilidade.

Não transformar cada bloco HTML em componente. Uma página deve continuar fácil de ler.

## 5. Estrutura proposta

```text
src/
├── app/
│   ├── layouts/
│   │   ├── AppLayout.vue
│   │   ├── AuthLayout.vue
│   │   └── AdminLayout.vue
│   ├── router/
│   │   ├── index.ts
│   │   └── guards.ts
│   └── App.vue
│
├── api/
│   ├── generated/
│   ├── adapters/
│   ├── client.ts
│   └── errors.ts
│
├── assets/
│   ├── fonts/
│   ├── icons/
│   └── images/
│
├── components/
│   ├── base/
│   │   ├── BaseButton.vue
│   │   ├── BaseCheckbox.vue
│   │   ├── BaseDialog.vue
│   │   ├── BaseFieldError.vue
│   │   ├── BaseIconButton.vue
│   │   ├── BaseInput.vue
│   │   ├── BaseSelect.vue
│   │   ├── BaseTextarea.vue
│   │   └── BaseToast.vue
│   ├── layout/
│   │   ├── AppHeader.vue
│   │   ├── MobileNavigation.vue
│   │   ├── PageContainer.vue
│   │   └── ThemeSwitch.vue
│   ├── publication/
│   │   ├── PublicationCard.vue
│   │   ├── PublicationHeader.vue
│   │   ├── PublicationImage.vue
│   │   ├── RecipeFlipCard.vue
│   │   ├── ReactionBar.vue
│   │   ├── SaveButton.vue
│   │   └── VisibilityBadge.vue
│   └── recipe/
│       ├── IngredientList.vue
│       ├── IngredientEditor.vue
│       ├── OriginReference.vue
│       └── RecipeInstructions.vue
│
├── composables/
│   ├── useTheme.ts
│   ├── useImagePreview.ts
│   ├── useRecipeFlip.ts
│   └── useProblemDetails.ts
│
├── features/
│   ├── auth/
│   ├── feed/
│   ├── publications/
│   ├── recipes/
│   ├── profile/
│   ├── saved/
│   ├── search/
│   ├── notifications/
│   ├── reports/
│   └── moderation/
│
├── mocks/
│   ├── fixtures/
│   ├── handlers/
│   └── browser.ts
│
├── stores/
│   ├── auth.store.ts
│   └── theme.store.ts
│
├── styles/
│   ├── reset.css
│   ├── tokens.css
│   ├── typography.css
│   ├── themes.css
│   └── global.css
│
├── views/
│   ├── RegisterView.vue
│   ├── LoginView.vue
│   ├── FeedView.vue
│   ├── SearchView.vue
│   ├── CreatePublicationView.vue
│   ├── PublicationDetailsView.vue
│   ├── ProfileView.vue
│   ├── SavedView.vue
│   ├── NotificationsView.vue
│   └── admin/
│       ├── ModerationQueueView.vue
│       └── ModerationCaseView.vue
│
├── main.ts
└── env.d.ts
```

Pastas vazias não devem ser criadas por antecipação. Criar cada pasta quando sua primeira implementação for necessária.

## 6. Rotas iniciais

| Rota | Acesso | Tela |
|---|---|---|
| `/cadastro` | Visitante | Cadastro |
| `/login` | Visitante | Login |
| `/` | Público | Feed; visitantes veem somente conteúdo `PUBLIC`. |
| `/buscar` | Público | Busca por título ou ingrediente. |
| `/publicacoes/:id` | Conforme visibilidade | Detalhes do prato ou da receita. |
| `/u/:username` | Público | Perfil público sem avatar ou estatísticas. |
| `/publicar` | Autenticado | Criar publicação. |
| `/publicar/minha-versao/:sourceId` | Autenticado | Criar `MY_VERSION` via **Fiz também**. |
| `/salvos` | Autenticado | Publicações salvas. |
| `/notificacoes` | Autenticado | Avisos do usuário. |
| `/admin/moderacao` | `ADMIN` | Fila administrativa. |
| `/admin/moderacao/:caseId` | `ADMIN` | Análise de um caso. |

Os guards do router melhoram a experiência, mas não substituem a autorização do backend.

## 7. Estado local e estado remoto

### Pinia

Usar Pinia apenas para estado global controlado pelo navegador:

- usuário autenticado e papel atual;
- estado de inicialização da sessão;
- preferência de tema;
- informações globais pequenas que não sejam cache de endpoints.

Não duplicar no Pinia coleções já controladas pelo Vue Query.

### Vue Query

Usar Vue Query para:

- feed e paginação;
- detalhes de publicação;
- perfil e publicações do perfil;
- busca;
- salvos;
- notificações;
- fila administrativa;
- estados de carregamento, erro e atualização;
- invalidação após reagir, salvar, publicar, denunciar ou moderar.

As `queryKey` devem ser centralizadas por feature. Após uma mutação, invalidar somente os recursos afetados.

## 8. Integração com a API

### 8.1 OpenAPI como fonte de verdade

O backend deverá disponibilizar um OpenAPI JSON ou YAML válido. O front consumirá essa especificação com Orval.

Exemplo de configuração inicial:

```ts
// orval.config.ts
import { defineConfig } from 'orval'

export default defineConfig({
  comesEBebes: {
    input: {
      target: process.env.OPENAPI_URL ?? './docs/api/openapi.json',
    },
    output: {
      mode: 'tags-split',
      target: './src/api/generated/endpoints.ts',
      schemas: './src/api/generated/models',
      client: 'vue-query',
      mock: true,
      clean: true,
    },
  },
})
```

O formato exato será ajustado quando o OpenAPI real estiver disponível.

Scripts esperados:

```json
{
  "scripts": {
    "api:generate": "orval --config orval.config.ts",
    "api:check": "npm run api:generate && git diff --exit-code -- src/api/generated"
  }
}
```

### 8.2 Código gerado

- Nunca editar `src/api/generated` manualmente.
- Não duplicar DTOs do OpenAPI em `src/types`.
- Adaptadores são permitidos para criar modelos de apresentação, nunca para esconder contrato incorreto.
- Se uma propriedade obrigatória estiver ausente na API, corrigir o backend/OpenAPI.
- Erros HTTP devem ser convertidos em uma representação única em `api/errors.ts`.

### 8.3 Paginação

- A API começa em `page=1`.
- Tamanho padrão: 20.
- Máximo: 50.
- O front nunca converte silenciosamente para página zero.
- A ordenação do feed é controlada pelo backend: `published_at DESC, id DESC`.

### 8.4 Imagens durante o desenvolvimento

O front não deve conhecer o fornecedor de armazenamento.

- Upload: enviar exatamente um `File` pelo contrato definido na API.
- Leitura: renderizar a URL recebida pelo backend.
- Desenvolvimento: a URL poderá apontar para arquivo local ou fixture.
- Produção futura: a mesma propriedade poderá conter uma URL temporária do GCS.
- Não concatenar bucket, object name ou domínio do GCS no front.
- Aceitar JPEG, PNG ou WebP, até 15 MB, e apresentar erro amigável antes do envio.
- O backend continua sendo a autoridade final da validação.

Quando a resposta indicar `PENDING_VALIDATION`, exibir estado de processamento e não inserir a publicação no feed como se já estivesse ativa.

## 9. Componentes principais

### `AppHeader`

- Logo tipográfico.
- Busca.
- Criar publicação.
- Salvos.
- Notificações.
- Theme switch.
- Username/menu da sessão.
- Não contém avatar.

### `PublicationCard`

- Suporta `DISH`, `RECIPE` e `MY_VERSION`.
- Renderiza autor sem avatar.
- Mantém fotografia como protagonista.
- Reutiliza `ReactionBar`, `SaveButton` e `RecipeFlipCard`.
- Não possui comentários, seguidores ou métricas de popularidade.

### `RecipeFlipCard`

- Frente: fotografia 4:5 e affordance para virar.
- Verso: título, rendimento e prévia de ingredientes.
- Nunca mostra preparo completo no verso.
- Nunca cria scroll interno.
- Exibe `+ N ingredientes` quando necessário.
- Possui `Ver receita completa` e `Voltar para a foto`.
- Vira somente por clique, toque, Enter ou Espaço; nunca por hover.
- Com `prefers-reduced-motion`, troca o conteúdo por crossfade sem rotação 3D.

### `ReactionBar`

- Reações persistidas: `WOULD_EAT`, `WANT_TO_MAKE` e `COMFORT_FOOD`.
- **Fiz também** é uma ação separada no domínio, mas pode ocupar a mesma barra visual.
- Selecionar novamente a mesma reação desfaz a seleção.
- Se o autor ocultar os totais, não mostrar números das três reações.
- A quantidade de versões de **Fiz também** permanece visível.
- Nunca mostrar identidades de quem reagiu.

### `IngredientEditor`

- Campos: nome, quantidade, unidade e observação opcional.
- Permite adicionar, remover e reordenar.
- A posição visual começa em 1.
- Gera a estrutura exata esperada pelo OpenAPI.

## 10. Formulários

- Usar estado tipado e validação previsível.
- Exibir erros por campo retornados pela API.
- Não depender somente da validação do navegador.
- Desabilitar submissão enquanto a requisição estiver em andamento.
- Impedir submissão duplicada.
- Preservar dados preenchidos após erro recuperável.

### Publicação

- Exatamente uma imagem.
- `DISH`: imagem obrigatória; título e descrição opcionais conforme as regras funcionais atuais.
- `RECIPE`: título, pelo menos um ingrediente e preparo obrigatórios.
- `MY_VERSION`: receita original, sufixo, imagem, ingredientes e preparo obrigatórios.
- Visibilidade obrigatória: `PUBLIC` ou `INTERNAL`.
- A imagem não poderá ser substituída depois da publicação.
- O preparo é um texto único; a apresentação separa linhas não vazias.

### Divergência visual conhecida

O planejamento do Visily descreveu a descrição de `DISH` como obrigatória. As regras de negócio e a modelagem atuais permitem publicação somente com a imagem. A implementação deve seguir a regra funcional; a tela deverá ser corrigida sem descaracterizar o design.

## 11. Tema e estilos

Os tokens ficam em CSS custom properties. Componentes não devem repetir valores hexadecimais.

```css
:root {
  --font-editorial: "Libre Caslon Text", Georgia, serif;
  --font-interface: Inter, system-ui, sans-serif;

  --color-background: #fff6e3;
  --color-surface: #f3f8f5;
  --color-text: #17281f;
  --color-primary: #285943;
  --color-secondary: #35758a;
  --color-accent: #d5a62e;
  --color-border: #d4ddd1;
}

[data-theme="dark"] {
  --color-background: #2d1a11;
  --color-surface: #341e14;
  --color-surface-raised: #40261b;
  --color-text: #fef5f1;
  --color-text-secondary: #cabeb9;
  --color-primary: #e99d7a;
  --color-accent: #b5cc79;
  --color-border: #978982;
}
```

Os tokens definitivos de espaçamento, tipografia, raio, sombra e transição serão extraídos das pranchas antes de implementar as páginas.

## 12. Responsividade

- Desktop de referência: 1440 px.
- Mobile de referência futura: 390 px.
- Feed desktop: coluna central com aproximadamente 720 px.
- Evitar alturas fixas para conteúdo textual.
- Evitar posicionamento absoluto estrutural.
- Usar grid e flexbox.
- O mobile terá navegação inferior com Início, Buscar, Publicar, Salvos e Perfil.
- Até as pranchas mobile serem geradas, implementar somente uma base responsiva conservadora; não inventar uma identidade mobile diferente.

## 13. Acessibilidade

- HTML semântico.
- Contraste mínimo WCAG AA.
- Foco visível.
- Labels reais para campos.
- Botões verdadeiros para ações.
- Texto alternativo útil para imagens.
- Navegação completa por teclado.
- Alvos de toque confortáveis no mobile.
- Respeitar `prefers-reduced-motion`.
- Estado nunca comunicado apenas por cor.

## 14. Testes

### Unitários

- Adaptadores e tratamento de erro.
- Composables com regras próprias.
- Formação de título de `MY_VERSION` quando existir lógica no front.
- Separação de linhas do preparo.

### Componentes

- `RecipeFlipCard` nos dois estados e por teclado.
- `ReactionBar` selecionando e desfazendo.
- `IngredientEditor` adicionando, removendo e reordenando.
- Formulários exibindo erros do backend.

### E2E

- Cadastro e login.
- Feed público e autenticado.
- Criar `DISH` e `RECIPE`.
- Iniciar `MY_VERSION` por **Fiz também**.
- Reagir e desfazer reação.
- Salvar e remover dos salvos.
- Denunciar publicação.
- Fluxo administrativo de moderação.

### Visual

- Comparar cada rota com sua referência em 1440 px.
- Quando disponíveis, comparar também em 390 px.
- Verificar claro e escuro.
- Não aprovar somente porque a aplicação compilou.

## 15. Ordem de implementação

1. Inicializar ferramentas e qualidade de código.
2. Importar OpenAPI e provar a geração do cliente.
3. Criar tokens, tipografia e temas.
4. Criar componentes `base`.
5. Criar layouts, header e navegação.
6. Implementar Cadastro e Login.
7. Implementar Feed e `PublicationCard`.
8. Implementar `RecipeFlipCard` e reações.
9. Implementar Detalhes da receita.
10. Implementar Criar publicação.
11. Implementar Perfil.
12. Implementar busca, salvos e notificações.
13. Implementar telas administrativas.
14. Gerar/adaptar mobile quando as referências estiverem disponíveis.

Cada etapa deve terminar com testes e comparação visual antes da próxima.

## 16. Critérios de conclusão

- Não existem DTOs manuais duplicando o OpenAPI.
- Componentes comuns não estão copiados entre páginas.
- Tema claro e escuro usam os mesmos componentes.
- A aplicação funciona com mocks e com a API real.
- Nenhuma tela adiciona avatar, comentários, mensagens, seguidores, ranking, nutrição, categorias ou funcionalidades fora do MVP.
- As telas foram verificadas no navegador contra as referências.
- Erros, carregamento, vazio e desabilitado estão representados.
- Testes relevantes passam.
