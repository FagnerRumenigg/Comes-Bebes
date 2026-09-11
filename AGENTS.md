# AGENTS.md

Guidance for coding agents when working in this repository.

## Idioma

Responda sempre em português (pt-BR), em qualquer mensagem para o usuário.

## Compress Tool Output

Ao executar ferramentas:

- Ignore cabeçalhos verbosos e informações desnecessárias.
- Trunque listas longas, mostrando apenas os primeiros itens e a quantidade restante.
- Omita confirmações de sucesso, exceto quando forem importantes.
- Use caminhos relativos e resuma saídas repetitivas.
- Mostre apenas resultados relevantes, erros e status importantes.

## Comunicação

- Mantenha respostas concisas e diretas.
- Evite floreios e explicações desnecessárias.
- Use listas quando ajudarem na leitura.
- Termine com uma linha resumindo o resultado.

## Infraestrutura e Deploy

- A produção usa Azure (Container Apps, ACR, Postgres Flexible Server e Blob Storage), desde a migração de 19/08/2026.
- Docker/Postgres local, incluindo `backend/docker-compose.yml` e a stack compartilhada `Infra-Geral`, serve apenas para testes locais.
- ngrok e Docker Hub estão aposentados neste projeto; não devem ser reintroduzidos.
- `docs/DEPLOY.md` é a fonte única sobre infraestrutura e deploy. Leia antes de alterações em infra ou CI/CD e atualize o documento na mesma mudança.
- O deploy de produção é automático a partir de uma tag Git (`api-v*` ou `validator-v*`). Não há etapa manual no servidor.

## Fluxo de Alterações Publicáveis

Depois de implementar uma alteração:

1. Inicie backend e frontend localmente, quando aplicável, e deixe-os rodando para testes.
2. Aguarde a aprovação explícita do usuário antes de fazer deploy.
3. Após a aprovação:
   - Integre na branch `main`, faça push e crie tags SemVer conforme necessário.
   - Registre toda correção ou feature nova na tabela `application.patch_notes`, para que o aviso apareça no site, seguindo `docs/DEPLOY.md`.
   - Atualize `docs/BACKLOG.md`, marcando o item como concluído e registrando o que foi entregue.

## Sem Pull Request

Este repositório usa integração direta na `main`, sem etapa de pull request. Cada item de backlog deve ter sua própria branch (`ideia/XXX-nome-curto`) e um commit de integração descritivo.
