# Regras para o agente que constrói o Comes&Bebes

Este arquivo é para você, agente. Leia inteiro antes de tocar em qualquer
tela. Ele não substitui os dois documentos do pacote — resume o que mais
tem causado erro e aponta onde ler o resto.

---

## 1. Antes de codificar qualquer coisa

Nesta ordem, sempre:

1. `documentos/Comes_Bebes_Produto_Experiencia_v5.docx` — o porquê. Lê pelo
   menos as seções 1 a 4 (princípio, o teste dos 64 anos, as sete regras
   invioláveis, o vocabulário) antes de escrever a primeira linha de
   interface. É o que evita reintroduzir um erro já corrigido.
2. `documentos/Comes_Bebes_Implementacao_Frontend_v10.docx` — o como.
   Tokens, componentes, validação, estados, navegação e a especificação de
   cada tela, seção por seção.
3. O arquivo HTML da tela em `telas/`, correspondente ao que você vai
   construir. **Ele vence quando divergir do texto do documento** — é
   código que roda e foi medido, o documento pode estar um passo atrás.

Nunca comece pela tela sem ler as regras. Foi assim que a marca virou um
ícone de garfo e faca inventado, e que "Minha versão" virou uma tela com
mais de quarenta campos repetidos — nenhuma das duas coisas estava em
nenhuma referência, foram suposições.

---

## 2. Como validar uma tela (procedimento, não impressão geral)

Validar não é abrir a tela e achar que está bonita. É passar cada um dos
itens abaixo, na ordem, e marcar explicitamente o que passou e o que não
passou. Se pular um item, diga que pulou — não declare a tela aprovada
sem ter checado.

**2.1 Texto, palavra por palavra**

- Todo texto novo confere com a tabela de vocabulário banido (seção 4 do
  documento de Produto): servidor, instância, deploy, ambiente, API,
  endpoint, cache, token, timeout, biometria, logout, revogar,
  dispositivo — nenhuma dessas palavras aparece em texto voltado a quem
  usa, em nenhuma tela, nem em mensagem de erro, nem em rótulo de botão.
- Nenhum contador público apareceu (curtidas, salvamentos, seguidores,
  quantidade de reações, quantidade de avisos). Contador de itens dentro
  de uma coleção e o contador de escolhas no seletor de reações são a
  única exceção — são inventário próprio, não aprovação alheia. Se tiver
  dúvida se um número é contador-placar ou inventário, é placar até prova
  do contrário.
- Nenhuma etiqueta de revista ("PERFIL PÚBLICO", "EDIÇÃO ESPECIAL" e
  parecidos).
- Se a tela tem estado vazio, ele tem as três partes exigidas: o que
  aconteceu, como mudar isso, e um caminho para sair (seção 3.7 do
  documento de Produto).
- Todo texto de erro segue a tela de erro única (seção 11.2 do documento
  de Implementação) — não invente um texto de erro novo fora dela.

**2.2 Os cinco estados obrigatórios**

Toda tela com formulário ou carregamento de dados precisa comprovar os
cinco estados da seção 7.4: vazio, preenchendo, carregando, erro, sucesso.
Se um deles não existir na tela que você construiu, a tela não está
pronta — não é opcional, é checklist.

**2.3 Cores e contraste**

- Nenhuma cor fora de `tokens.css`. Se precisar de uma cor nova, pare e
  peça — não invente hex.
- `--accent` nunca é texto nem borda, só preenchimento (regra 1 do
  `tokens.css`).
- Botão primário usa `var(--on-primary)`, nunca branco fixo (regra 2).
- Todo campo e card tem borda visível, nunca só preenchimento (regra 3).

**2.4 Navegação e foco**

- Nenhum ícone sem rótulo escrito ao lado, exceto o avatar no topo do
  celular — e mesmo esse tem `aria-label`.
- Estado ativo da navegação é marcado por peso de fonte + traço, nunca só
  por cor, e leva `aria-current="page"`.
- Botão principal do formulário nunca fica desabilitado — ver seção 7.1
  do documento de Implementação. Se você desabilitou um botão de envio em
  algum formulário, isso é erro, não estilo.

**2.5 Ao final, registre**

Depois de rodar 2.1 a 2.4, escreva explicitamente: o que foi checado, o
que passou, o que não passou e o que ficou pendente. Uma tela "parece
estar de acordo com o guia" não é validação — é chute educado.

---

## 3. Cabeçalho e rodapé (App shell)

**Regra central: cabeçalho web / barra inferior do celular aparecem em
toda tela depois que a pessoa está autenticada, e somente nelas.**

Especificação completa na seção 12 do documento de Implementação
(`04-navegacao.html` é a referência visual).

**Telas SEM cabeçalho/rodapé** — a pessoa ainda não entrou:

| Tela | Arquivo |
|---|---|
| Boas-vindas / erro | `01-boas-vindas-e-erro.html` |
| Login e cadastro | `02-login-e-cadastro.html` |
| Carregando (cold start) | `03-carregando.html` |
| Recuperar senha | `11-recuperar-senha.html` |

**Telas COM cabeçalho/rodapé** — a pessoa já está autenticada:

| Tela | Arquivo |
|---|---|
| Feed | `05-feed.html` |
| Salvos | `06-salvos.html` |
| Coleção | `07-colecao.html` |
| Perfil | `08-perfil.html` |
| Configurações | `09-configuracoes.html` |
| Publicar | `10-publicar.html` |
| Avisos | `12-avisos.html` |
| Minha versão | `13-minha-versao.html` |
| Editar perfil | `14-editar-perfil.html` |

Web: cabeçalho fixo de 70px (marca, Início · Salvos · Avisos, botão
Publicar, avatar+nome — ver 12.1). Celular: topo de 58px só com marca e
avatar, mais barra inferior de 5 itens fixos — Início · Salvos · Publicar
· Avisos · Perfil (ver 12.2). É o mesmo componente de app shell em toda
tela autenticada; a tela muda por dentro, o shell não muda.

Se uma tela nova precisar decidir se algo entra na barra ou vira filtro,
use o teste da seção 12.3 / 3.3: "vou ver X" é lugar (pode precisar de
porta de entrada, não necessariamente vaga na barra); "quero ver só os X"
é lente (vira filtro, nunca item de navegação).

---

## 4. Fluxo completo do produto

Isto é a ordem real das telas, do primeiro toque até o uso normal. Sem
isso o agente tende a tratar cada tela como uma página solta, e a costura
entre elas é onde mais aparece bug de navegação.

```
App aberto, sem sessão válida
  └─ 02 Login e cadastro
       ├─ "Esqueci minha senha" → 11 Recuperar senha (5 passos + link expirado)
       │      └─ senha trocada → volta para 02, com o e-mail já preenchido
       └─ Entrar ou Criar conta
              └─ 03 Carregando (estágios de 3s / 12s / 30s / 80s)
                     ├─ falha → tela de erro unificada (dentro de 01, causa
                     │          "server" ou "unknown" — ver 11.3)
                     ├─ é o primeiro login desta conta
                     │      └─ 01 Boas-vindas ("Começar a explorar")
                     │             └─ 05 Feed — autenticado, shell completo
                     └─ não é o primeiro login
                            └─ 05 Feed — direto, autenticado, shell completo

A partir daqui a pessoa está autenticada. Toda tela abaixo tem cabeçalho/
rodapé (seção 3 deste arquivo) e é alcançável a partir do Feed ou da
navegação, não numa sequência fixa:

  05 Feed ── 06 Salvos ── 07 Coleção (aberta a partir de Salvos ou de um perfil)
     │            │
     │            └─ organizar / criar coleção (folha, não tela nova)
     │
     ├─ 08 Perfil (próprio ou de outra pessoa)
     │      └─ próprio → "Editar perfil" → 14 Editar perfil
     │
     ├─ 09 Configurações (a partir do menu da conta)
     │
     ├─ 10 Publicar (botão central/primário, disponível em toda tela autenticada)
     │      └─ sucesso → volta para onde a pessoa estava, publicação já visível
     │
     ├─ 12 Avisos
     │      └─ aviso de coleção nova → 07 Coleção
     │      └─ aviso de "guardaram sua receita" → a publicação
     │
     └─ 13 Minha versão (a partir de uma publicação de outra pessoa,
            nunca da navegação principal — é lugar sem vaga na barra,
            mesmo raciocínio de Coleções em 12.3)

  "Sair da conta" (dentro do menu da conta, com confirmação) → volta para 02,
  sem sessão.
```

Pontos que já causaram confusão e por isso ficam registrados aqui:

- Boas-vindas (01) **não é a primeira tela do app**. É a primeira tela
  *depois* do primeiro login bem-sucedido. Quem abre o app pela primeira
  vez cai direto em 02 (Login e cadastro).
- Confirmação de e-mail não bloqueia nada do fluxo acima — a pessoa usa o
  produto normalmente sem confirmar, e só é cobrada ao tentar publicar
  (seção 5.4 do documento de Produto, seção 21.3 do de Implementação).
- A tela de carregando (03) e a tela de erro (01) são o mesmo componente
  por trás — ver seção 11.3. Não construa uma tela de erro nova para o
  cold start.
- "Minha versão" e "Publicar" são a mesma tela com pequenas variações
  (seção 21.6), não dois componentes separados.

---

## 5. Precedência quando algo diverge

Ordem de autoridade, do mais forte para o mais fraco:

1. O arquivo HTML de referência em `telas/` — comportamento e texto
   exatos.
2. Este arquivo (`REGRAS_AGENTE.md`) — processo e fluxo.
3. Os dois `.docx` — a razão por trás de cada decisão, e o que cobre o
   que não cabe numa tela de exemplo.

Se um `.docx` disser uma coisa e a tela de referência mostrar outra, a
tela vence — e vale avisar que os dois estão desalinhados, para alguém
atualizar o documento depois.

Se nenhum dos três resolver a dúvida, o critério final é a seção 8 do
documento de Produto ("Critérios de avaliação") e o princípio central da
seção 1: entre a solução tecnicamente elegante e a que exige menos da
pessoa, escolher a segunda. Se mesmo assim ficar em dúvida, pare e
pergunte — não decida sozinho uma regra de produto nova.

---

## 6. Erros já cometidos — não repetir

- Inventar elemento visual (ícone, cor, mascote) sem checar a referência
  real. A marca já foi inventada errada uma vez.
- Campos repetíveis com botões de adicionar/remover/reordenar para
  ingrediente e passo de receita. Foi tentado, causou mais de quarenta
  elementos interativos numa tela e foi substituído por texto livre em
  duas telas (Publicar e Minha versão). Não reintroduzir esse padrão em
  nenhuma tela nova sem decisão explícita.
- Botão de ação que fica desabilitado até o formulário estar "correto".
  Proibido — ver 2.4 acima e seção 7.1 do documento de Implementação.
- Pedir para a pessoa confirmar algo que o sistema consegue checar
  sozinho (ex.: botão "Já confirmei o e-mail"). Ver seção 3.6 do
  documento de Produto.
- Adicionar contador em qualquer lugar novo do produto sem antes checar a
  seção 3.1. É a regra mais fácil de violar sem perceber, porque contador
  é o padrão automático de quem já construiu outros apps sociais.
