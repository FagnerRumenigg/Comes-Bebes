# Comes&Bebes — pacote de design

Consolidado em 20 de agosto de 2026. Quinze telas, dois documentos, as
regras para o agente, os tokens e a marca.

---

## Se você é o agente que vai construir o site

Leia `REGRAS_AGENTE.md` inteiro antes de tocar em qualquer tela. É onde
está o passo a passo de como validar uma tela (não é "achar que está
bonita"), quando o cabeçalho e o rodapé aparecem, e o fluxo completo do
produto — que tela leva a qual, e o que já foi tentado e revertido.

---

## Como usar

Abra qualquer arquivo de `telas/` direto no navegador — duplo clique, sem
servidor, sem instalar nada.

Toda tela tem uma **barra escura no topo** para alternar plataforma, tema e
estado. Ela é ferramenta de revisão e **não faz parte do produto**.

Todas são responsivas de verdade: arraste a janela e o layout muda.

---

## telas/

Numeradas na ordem do fluxo, do primeiro contato ao fundo do produto.

| Arquivo | O que tem dentro |
|---|---|
| `00-marca.html` | Três tratamentos do logotipo. O A é o escolhido. |
| `01-boas-vindas-e-erro.html` | Primeiro acesso e a tela de erro, com três causas |
| `02-login-e-cadastro.html` | Entrada e cadastro, com todos os estados |
| `03-carregando.html` | A cafeteira e os quatro estágios de espera |
| `04-navegacao.html` | Cabeçalho web, barra do celular, menu da conta |
| `05-feed.html` | Feed, busca, filtro, reações e o gesto de guardar |
| `06-salvos.html` | Salvos com coleções, nos três estados |
| `07-colecao.html` | Coleção aberta, privacidade e compartilhar |
| `08-perfil.html` | Perfil próprio e de outra pessoa |
| `09-configuracoes.html` | Cinco seções de configuração |
| `10-publicar.html` | Publicar, com sete estados |
| `11-recuperar-senha.html` | Cinco passos, incluindo link expirado |
| `12-avisos.html` | Lista de avisos, menu de cada item e limpar tudo |
| `13-minha-versao.html` | Publicar a partir de outra receita, com cartão de origem e título de prefixo travado |
| `14-editar-perfil.html` | Nome, @usuário e descrição, com aviso condicional de troca de @ |

**Vale abrir e mexer:** o filtro do feed abre de verdade; o seletor de
reações recusa a quarta escolha e explica; guardar uma publicação e tocar de
novo no mesmo botão mostra a folha de coleções; em `07`, trocar o nível de
privacidade muda o modal de compartilhar junto; em `11`, o contador de
reenvio corre; em `12`, o menu de um aviso de coleção tem uma opção a mais
que os outros; em `13`, o título muda ao vivo enquanto se digita o
complemento; em `14`, o aviso de troca de @ só aparece quando o valor
digitado é diferente do original.

---

## documentos/

**`Comes_Bebes_Produto_Experiencia_v5.docx`** — o *porquê*. Princípios, as
regras invioláveis, o vocabulário aprovado e banido, as decisões de
produto e os critérios de avaliação de telas novas. É o documento que se
consulta quando aparece uma dúvida que nenhuma especificação prevê.

**`Comes_Bebes_Implementacao_Frontend_v10.docx`** — o *como*. Tokens,
componentes Vue com código, validação, estados, acessibilidade, a regra de
geração do @usuário, checklist de revisão e a tabela de pendências técnicas.

**Precedência:** quando um documento e uma tela divergirem, a tela vence —
ela é código que roda e foi medido. Os documentos explicam decisões e cobrem
o que não cabe num arquivo de exemplo.

---

## tokens.css

O bloco de cores dos dois temas, pronto para `src/assets/styles/tokens.css`,
importado uma vez no `main.js`.

**Três regras que não podem ser quebradas:**

1. O dourado `--accent` é preenchimento. Nunca texto, nunca borda — sobre o
   creme ele dá 2,1:1 e reprova.
2. Botão primário usa `var(--on-primary)`, nunca `#fff`. No tema escuro o
   primário é claro, e texto branco em cima dele dá 2,2:1.
3. Campo e card precisam de borda. `--surface` e `--bg` têm contraste 1,0:1
   entre si: preenchimento sozinho não delimita nada.

---

## marca.svg

O livro dourado sobre azulejo verde. Cores fixas no arquivo; dentro da
aplicação use as classes de `tokens.css` para ele acompanhar o tema.
Tamanho mínimo 20px — é o do topo do celular, e é onde conferir primeiro.

---

## O que falta

**Telas:** nenhuma. As quinze estão desenhadas.

**Produto:** fotografias definitivas dos painéis, avatares de cozinha, e
igualar o limite de foto entre Publicar (8 MB, JPG/PNG) e Minha versão (30
MB, JPEG/PNG/WebP/HEIC/HEIF) — hoje são regras diferentes por herdarem de
telas construídas em momentos diferentes.

**Backend:** implementar a regra de geração e colisão do @usuário (19.4 do
documento de implementação), confirmar que o ping de 4 minutos impede a
escala a zero, e o teto de agrupamento dos avisos.

**E o mais importante:** repetir o teste com a usuária de 64 anos. Ela é o
critério final de tudo que está aqui.
