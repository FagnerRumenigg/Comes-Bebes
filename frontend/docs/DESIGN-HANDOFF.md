# Comes&Bebes — Handoff de Design para Implementação

## 1. Controle do documento

| Campo | Valor |
|---|---|
| Status | Referência visual para implementação |
| Versão | 1.0 |
| Data | 08/08/2026 |
| Ferramenta de origem | Visily |
| Prancha pública | https://app.visily.ai/projects/a0b4b494-a000-4e4a-a3e5-222ba3f97f00/boards/2688099 |
| Responsáveis | Fagner e ChatGPT |

## 2. Objetivo

Orientar um agente de implementação a reproduzir fielmente as telas aprovadas do **Comes&Bebes** dentro do projeto Vue, utilizando componentes e tokens compartilhados.

O link do Visily é uma referência complementar. As imagens exportadas e armazenadas no repositório serão a fonte visual principal porque são estáveis, versionáveis e podem ser comparadas automaticamente.

Este documento não autoriza alterar regras do produto. Em caso de conflito:

1. Regras de negócio e OpenAPI vencem sobre conteúdo ilustrativo do design.
2. Imagens exportadas vencem sobre interpretações livres de estilo.
3. Tokens e componentes existentes vencem sobre a criação de sistemas paralelos.

## 3. Preparação dos arquivos de referência

Exportar as telas do Visily como PNG, preferencialmente na resolução original ou em 2x, sem barras e controles do editor.

Estrutura esperada:

```text
docs/design/
├── desktop-light/
│   ├── cadastro.png
│   ├── login.png
│   ├── feed.png
│   ├── criar-publicacao.png
│   ├── perfil-publico.png
│   └── detalhes-receita.png
├── desktop-dark/
│   ├── cadastro.png
│   ├── login.png
│   ├── feed.png
│   ├── criar-publicacao.png
│   ├── perfil-publico.png
│   └── detalhes-receita.png
├── mobile-light/
├── mobile-dark/
├── assets/
│   ├── foto-risoto.webp
│   ├── foto-bolo.webp
│   ├── foto-bolo-minha-versao.webp
│   └── foto-pao.webp
├── DESIGN-HANDOFF.md
└── FRONTEND-ARCHITECTURE.md
```

Enquanto as versões mobile não existirem, as duas pastas mobile poderão permanecer vazias. O agente não deve declarar fidelidade visual mobile sem referências.

### Não fazer

- Não usar screenshots que incluam o canvas inteiro com todas as telas minúsculas.
- Não depender apenas do link público.
- Não recortar fotografias a partir do screenshot se o arquivo original estiver disponível.
- Não renomear arquivos de forma ambígua como `tela1-final-agora-vai.png`.

## 4. Identidade visual

### Conceito

Revista gastronômica independente e jornal de domingo. A fotografia de comida é a protagonista. A interface transmite calor, curiosidade, conforto e autenticidade sem parecer rede social genérica, aplicativo de delivery ou supermercado orgânico.

### Elementos constantes

- Logo tipográfico `Comes&Bebes`.
- Libre Caslon Text, ou a fonte editorial aprovada, para títulos.
- Inter para interface e texto funcional.
- Fotografias grandes e autênticas.
- Espaçamento generoso.
- Superfícies inspiradas em papel.
- Bordas e sombras discretas.
- Ícones simples e consistentes.
- Nenhum avatar ou foto pessoal.

### Tema claro

| Uso | Cor |
|---|---|
| Fundo principal | `#FFF6E3` |
| Superfície | `#F3F8F5` |
| Texto principal | `#17281F` |
| Verde principal | `#285943` |
| Azul secundário | `#35758A` |
| Amarelo de destaque | `#D5A62E` |
| Borda | `#D4DDD1` |

O claro deve lembrar floresta, água, sol e papel impresso sem parecer produto ecológico ou infantil.

### Tema escuro

| Uso | Cor |
|---|---|
| Fundo principal | `#2D1A11` |
| Superfície | `#341E14` |
| Superfície elevada | `#40261B` |
| Texto principal | `#FEF5F1` |
| Texto secundário | `#CABEB9` |
| Pêssego principal | `#E99D7A` |
| Verde de destaque | `#B5CC79` |
| Borda | `#978982` |

O escuro deve parecer a mesma revista lida à noite, com tons de café, terracota e pêssego. Não usar estética gamer, neon excessivo ou cyberpunk.

## 5. Inventário de telas

### Disponíveis

| Tela | Desktop claro | Desktop escuro | Mobile |
|---|---:|---:|---:|
| Cadastro | Sim | Sim | Pendente |
| Login | Sim | Sim | Pendente |
| Feed | Sim | Sim | Pendente |
| Criar publicação | Sim | Sim | Pendente |
| Perfil público | Sim | Sim | Pendente |
| Detalhes da receita | Sim | Sim | Pendente |

O conteúdo, os componentes e os estados devem permanecer equivalentes entre claro e escuro. Alterar somente tokens semânticos de tema.

## 6. Mapa de telas e componentes

### Cadastro

Componentes:

- `AuthLayout`.
- `AppWordmark`.
- `BaseInput`.
- `PasswordInput`.
- `BaseButton`.
- `ThemeSwitch`.

Campos:

- Nome de exibição.
- Username.
- Senha.
- Confirmar senha.

A fotografia editorial deve ser pequena e não competir com o formulário. No mobile poderá ser removida.

O contrato vigente não recebe e-mail nem foto de perfil. Embora esses elementos apareçam em algumas pranchas, não devem ser implementados. Login social também permanece fora do MVP.

### Login

Componentes:

- `AuthLayout`.
- `AppWordmark`.
- `BaseInput`.
- `PasswordInput`.
- `BaseButton`.
- `ThemeSwitch`.

Ações:

- Entrar.
- Criar uma conta.

Recuperação de senha e login social não possuem endpoints no contrato vigente e não devem ser exibidos nesta etapa.

### Feed

Componentes:

- `AppLayout`.
- `AppHeader`.
- `MobileNavigation`.
- `PageContainer`.
- `PublicationCard`.
- `RecipeFlipCard`.
- `ReactionBar`.
- `SaveButton`.

Regras visuais:

- Feed cronológico e centralizado.
- Aproximadamente 720 px no desktop.
- Uma publicação grande por linha.
- Sem sidebar, tendências, recomendações ou anúncios.
- A foto é o maior elemento de cada publicação.
- Autor é exibido por nome e username, nunca avatar.

Publicações visuais de referência:

1. `DISH`: Risoto de Cogumelos.
2. `RECIPE`: Bolo de Cenoura com o card na frente.
3. `MY_VERSION`: Bolo de Cenoura — Cobertura de Chocolate.
4. `RECIPE`: Pão de Fermentação Natural com o card no verso.

### Criar publicação

Componentes:

- `PublicationTypeSelector`.
- `ImageUploader`.
- `VisibilitySelector`.
- `IngredientEditor`.
- `OriginSelector`.
- `BaseInput`, `BaseTextarea` e `BaseButton`.

Estados por tipo:

- `DISH`: imagem; título e descrição opcionais; visibilidade.
- `RECIPE`: imagem, título, ingredientes, preparo, rendimento e visibilidade.
- `MY_VERSION`: receita original, sufixo do título, nova imagem, ingredientes, preparo, rendimento e visibilidade.

Correção em relação à prancha: descrição de `DISH` não é obrigatória segundo as regras vigentes.

### Perfil público

Componentes:

- `AppLayout`.
- `ProfileIdentity`.
- `PublicationCard`.

Mostrar somente:

- Nome de exibição.
- Username.
- Publicações.

Não mostrar avatar, biografia, localização, quantidade de publicações ou estatísticas. Se a prancha contiver algum desses itens, remover durante a implementação.

### Detalhes da receita

Componentes:

- `AppLayout`.
- `PublicationImage`.
- `OriginReference` quando aplicável.
- `IngredientList`.
- `RecipeInstructions`.
- `ReactionBar`.
- `SaveButton`.
- `ReportDialog`.

Manter a composição de artigo editorial, com foto grande, título forte, autor sem avatar, rendimento, ingredientes e preparo completo.

## 7. Interações que a imagem não explica

### Card de receita

- O clique ou toque na fotografia vira o card.
- Enter e Espaço também viram quando o card possui foco.
- Hover não vira o card.
- Duração visual aproximada: 450 ms.
- Reações permanecem fora da área que vira.
- O verso contém somente título, rendimento e prévia dos ingredientes.
- O verso nunca contém preparo completo.
- O verso nunca possui scroll interno.
- Ingredientes excedentes aparecem como `+ N ingredientes`.
- `Ver receita completa` navega para os detalhes.
- `Voltar para a foto` restaura a frente.
- Com movimento reduzido, usar crossfade.

### Reações

- `Eu comeria`, `Quero fazer` e `Comida afetiva` são alternáveis.
- Selecionar novamente desfaz.
- Um usuário pode selecionar reações diferentes simultaneamente.
- Não existe coração genérico.
- Não mostrar identidades.
- Totais podem ser ocultados pelo autor.

### Fiz também

- Aparece na mesma barra visual, mas não é reação persistida.
- Exibe a quantidade de publicações derivadas.
- Ao clicar, explicar que o usuário precisará cadastrar sua receita.
- Após confirmação, navegar para `/publicar/minha-versao/:sourceId`.
- Não aumentar a contagem antes da publicação ser concluída.

### Salvar

- Estado selecionado e não selecionado.
- A lista de salvos é privada.
- Alteração otimista é permitida desde que reverta em caso de erro.

### Denunciar

- Ação discreta.
- Abre diálogo com motivo obrigatório e descrição opcional.
- Não mostrar novamente como disponível se o usuário já denunciou aquela publicação.

## 8. Estados obrigatórios

Cada tela ou feature deverá considerar, quando aplicável:

- carregando;
- vazio;
- erro recuperável;
- erro de autorização;
- desabilitado;
- selecionado;
- submissão em andamento;
- sucesso;
- publicação em validação de imagem;
- conteúdo em análise e indisponível;
- URL de imagem expirada, permitindo nova consulta ao backend.

Não inventar layouts totalmente diferentes para esses estados. Reutilizar os mesmos componentes e tokens.

## 9. Conteúdo proibido

Não criar:

- avatar ou foto de perfil;
- comentários;
- mensagens privadas;
- seguidores ou seguindo;
- biografia, localização ou especialidade;
- ranking, popularidade, tendências ou recomendações;
- nutrição;
- categorias, temas ou hashtags;
- anúncios;
- múltiplas imagens;
- coração genérico;
- estatísticas de perfil;
- componentes de delivery ou restaurante;
- funcionalidades não previstas pelo MVP.

## 10. Processo de implementação pelo agente

### Etapa 1 — leitura

Antes de editar código, o agente deve ler:

1. `FRONTEND-ARCHITECTURE.md`.
2. `DESIGN-HANDOFF.md`.
3. Regras de negócio.
4. OpenAPI atual.
5. Imagens da tela que será implementada.

### Etapa 2 — componentes e tokens

Antes de criar páginas completas:

1. Extrair tokens da referência.
2. Criar componentes base usados pela tela.
3. Implementar a tela usando esses componentes.
4. Não criar duplicatas se um componente já puder ser estendido por props ou slots simples.

### Etapa 3 — integração

1. Consumir o cliente gerado pelo OpenAPI.
2. Usar mocks gerados para trabalhar sem backend quando necessário.
3. Não inventar respostas nem propriedades fora do contrato.
4. Não acoplar o componente visual diretamente à estrutura de armazenamento de imagem.

### Etapa 4 — validação visual

Para cada tela:

1. Executar a aplicação.
2. Abrir no navegador em 1440 px.
3. Comparar com a imagem de referência do mesmo tema.
4. Corrigir hierarquia, largura, espaçamento, tipografia e recortes.
5. Repetir no tema oposto.
6. Quando existirem referências mobile, repetir em 390 px.
7. Testar também comportamento e teclado.

Compilar sem erros não significa que o design foi reproduzido.

## 11. Prompt inicial recomendado para o agente

```text
Leia integralmente FRONTEND-ARCHITECTURE.md, DESIGN-HANDOFF.md, as regras de
negócio e o OpenAPI atual antes de editar código.

Implemente somente a etapa/tela solicitada. Use as imagens em docs/design como
fonte visual, traduza o design para os tokens e componentes compartilhados do
projeto e não crie um sistema visual paralelo.

Consuma apenas os tipos e endpoints gerados pelo OpenAPI. Não escreva DTOs
manuais para contornar divergências. Preserve os padrões existentes de rotas,
estado e acesso a dados.

Depois de implementar, execute os testes relevantes e abra a aplicação no
navegador. Compare a tela em 1440 px com as referências clara e escura e ajuste
até que layout, espaçamento, tipografia, hierarquia e comportamento estejam
próximos. Não considere a tarefa concluída apenas porque compilou.

Não adicione funcionalidades fora do MVP. Se uma referência visual contrariar
uma regra de negócio ou o OpenAPI, preserve a regra e relate a divergência.
```

## 12. Checklist por tela

- [ ] Usa o layout compartilhado correto.
- [ ] Não duplica header, navegação ou componentes base.
- [ ] Consome o cliente OpenAPI gerado.
- [ ] Possui carregamento, erro e vazio quando aplicável.
- [ ] Tema claro confere com a referência.
- [ ] Tema escuro confere com a referência.
- [ ] Navegação por teclado funciona.
- [ ] Não contém funcionalidades proibidas.
- [ ] Testes relevantes passam.
- [ ] Foi verificada visualmente no navegador.
