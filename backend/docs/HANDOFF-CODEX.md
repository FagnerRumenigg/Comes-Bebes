# Handoff para o Codex — Rede Social de Comida

## 1. Objetivo deste arquivo

Este documento transfere para uma nova sessão do Codex o contexto necessário para continuar o projeto dentro do IntelliJ sem depender do histórico do chat original.

Ele não substitui os documentos completos. Antes de propor código, migrations ou alterações estruturais, leia integralmente, nesta ordem:

1. `regras-de-negocio-rede-social-comida.md`
2. `modelagem-banco-rede-social-comida.md`
3. este arquivo

## 2. Estado atual

- As regras funcionais do MVP foram discutidas e documentadas.
- A modelagem lógica do banco está encerrada na versão 0.8.
- Ainda não foi criado o modelo físico definitivo.
- Ainda não foram escritas as migrations do Flyway.
- Ainda não foram criadas entidades JPA, repositories, services ou controllers.
- O próximo trabalho deve começar pelo modelo físico do PostgreSQL.

O documento de regras de negócio foi preservado durante a fase de banco, conforme decisão do autor. A modelagem do banco contém refinamentos aprovados posteriormente, principalmente sobre moderação, denúncias, anonimização, validação de imagens e paginação. Quando ela for mais específica sobre esses assuntos, considere-a a decisão mais recente. Em caso de conflito real, pare e pergunte ao autor.

## 3. Stack definida

| Camada | Tecnologia |
|---|---|
| Backend | Java 25 |
| Framework | Spring Boot 4 |
| API | REST |
| Banco | PostgreSQL 17 |
| Migrations | Flyway |
| Frontend | Vue |
| Imagens | Google Cloud Storage |
| Validação inicial de imagens | Google Cloud Vision |

## 4. Visão resumida do produto

Rede social focada em fotos de comidas e receitas, sem comentários, mensagens privadas, seguidores ou fotos de pessoas.

Tipos de publicação:

- `DISH`: prato com foto; receita não obrigatória.
- `RECIPE`: receita completa com ingredientes e preparo.
- `MY_VERSION`: receita completa inspirada em outra receita, preservando a origem e o título original como prefixo.

Visibilidade:

- `PUBLIC`: acessível sem autenticação.
- `INTERNAL`: acessível somente a usuários autenticados.

Perfis do sistema:

- `USER`
- `ADMIN`

Não haverá perfil separado de moderador no MVP. O único administrador inicial será o autor do projeto.

## 5. Decisões críticas que não devem ser perdidas

### Publicações e receitas

- Cada publicação possui exatamente uma imagem.
- A imagem não pode ser substituída depois da publicação.
- `DISH` e `RECIPE` podem ser convertidos entre si sem apagar fisicamente os dados de receita.
- `MY_VERSION` pode virar `DISH`, mas mantém o vínculo, o título em homenagem à receita original e a contagem em **Fiz também**.
- **Fiz também** não é uma reação: inicia a criação de `MY_VERSION`.
- Ingredientes são estruturados e ordenados; o preparo é um texto único, com um passo por linha.

### Reações e relações sociais

- Reações iniciais: **Eu comeria**, **Quero fazer** e **Comida afetiva**.
- O usuário pode aplicar várias reações diferentes, mas apenas uma de cada tipo por publicação.
- Clicar novamente na mesma reação desfaz logicamente a reação.
- O autor pode ocultar totais de reações; o padrão é mostrar.
- A identidade de quem reagiu nunca é exibida.
- Não existem comentários, mensagens ou seguidores no MVP.

### Moderação e denúncias

- Cada usuário pode denunciar a mesma publicação apenas uma vez.
- A denúncia possui motivo estruturado e descrição opcional.
- O usuário pode criar no máximo dez denúncias em uma janela móvel de 24 horas.
- Três denúncias pendentes alteram a publicação para `UNDER_REVIEW` e a retiram do feed, busca e perfil público.
- A fila e as decisões de moderação são exclusivas de `ADMIN`.
- Se o conteúdo for aprovado, volta para `ACTIVE` e os denunciantes recebem `REPORT_REJECTED_WARNING`.
- O aviso informa que denúncias repetidamente rejeitadas podem desativar a conta, sem divulgar ou aplicar um limite automático no MVP.
- Bloquear, ocultar ou remover exige justificativa administrativa.

### Contas e exclusões

- Exclusões comuns são lógicas.
- Usuário bloqueado fica anônimo publicamente, não pode usar o sistema e mantém suas publicações existentes, salvo decisão administrativa específica.
- O e-mail de uma conta bloqueada não pode ser usado para novo cadastro.
- Para isso, o e-mail normalizado original é convertido em HMAC; a chave fica fora do banco.
- Uma exclusão voluntária permite escolher entre manter as publicações anonimizadas ou apagar fisicamente a conta e suas publicações.
- Na exclusão voluntária, o e-mail fica livre para novo cadastro.
- Versões criadas por terceiros sobrevivem à exclusão da receita original usando o snapshot do título.

### Imagens

- Entrada: JPEG, PNG ou WebP.
- Máximo: 5 MB e 20 megapixels.
- Saída normalizada: WebP, maior lado com até 1.600 pixels.
- Bucket privado, separado por ambiente, inicialmente em `us-east1`.
- O banco guarda bucket, object name, generation e metadados; nunca guarda URL assinada.
- URLs assinadas de leitura duram uma hora.
- Falhas no provedor recebem até três tentativas; depois seguem para análise administrativa.
- Resultado incerto não reprova automaticamente.

### Paginação

- A API REST começa em `page=1`.
- Tamanho padrão: 20.
- Tamanho máximo: 50.
- Configuração esperada: `spring.data.web.pageable.one-indexed-parameters=true`.
- Nunca retornar `Page` diretamente: usar DTO próprio e devolver `page.getNumber() + 1`.

## 6. Entidades lógicas já definidas

- `users`
- `publications`
- `publication_image_checks`
- `recipes`
- `recipe_ingredients`
- `publication_origins`
- `reaction_types`
- `publication_reactions`
- `saved_publications`
- `report_reasons`
- `reports`
- `moderation_cases`
- `app_config`
- `user_notifications`

Campos, estados, constraints propostas, índices e fluxos transacionais estão detalhados em `modelagem-banco-rede-social-comida.md`. Não recrie a modelagem a partir deste resumo.

## 7. Próxima tarefa recomendada

Criar e validar o modelo físico antes de escrever código Java:

1. Fechar os tipos PostgreSQL de cada coluna.
2. Definir todas as PKs, FKs e políticas `ON DELETE`.
3. Escrever `CHECK`, índices únicos e índices parciais.
4. Resolver a ordem real de criação das tabelas.
5. Produzir um diagrama ER físico atualizado.
6. Dividir o esquema em migrations Flyway pequenas e executáveis.
7. Somente depois criar entidades JPA e casos de uso.

Não gere todas as migrations de uma vez sem antes apresentar o modelo físico para aprovação do autor.

## 8. Regras de colaboração

- Não altere decisões aprovadas silenciosamente.
- Quando encontrar inconsistência, apresente o impacto e pergunte antes de escolher.
- Não adicione funcionalidades fora do MVP por antecipação.
- Prefira constraints no PostgreSQL para integridade estrutural e aplicação para regras transacionais ou de autorização.
- Preserve exclusão lógica e dados de moderação, exceto no expurgo físico explicitamente solicitado pelo usuário.
- Não guarde senha, e-mail bloqueado em texto puro, imagem binária ou URL assinada no PostgreSQL.
- Explique as decisões em português e mantenha nomes físicos em inglês com `snake_case`.

## 9. Prompt inicial sugerido para o IntelliJ

```text
Leia integralmente os arquivos:

- docs/regras-de-negocio-rede-social-comida.md
- docs/modelagem-banco-rede-social-comida.md
- docs/HANDOFF-CODEX.md

Eles contêm as decisões aprovadas do projeto. Não altere regras definidas sem
me consultar e não escreva migrations ainda.

Primeiro, revise a modelagem lógica e proponha o modelo físico definitivo para
PostgreSQL 17, incluindo tipos, PKs, FKs, políticas ON DELETE, CHECKs, índices
únicos e índices parciais. Aponte inconsistências ou decisões que ainda precisem
da minha confirmação.
```

---

## 10. Novas tarefas de infraestrutura

### Tarefa 1 — Criar validador síncrono local de imagens

Implementar um pipeline Python, executado localmente como processo separado ou sidecar, para validar imagens sem depender do GCS.

Proposta:

1. A API recebe o arquivo e o mantém em uma área de quarentena não pública.
2. O Java valida tamanho, dimensões e decodificação básica.
3. O Python recebe o caminho ou bytes da imagem e retorna `FOOD`, `NOT_FOOD` ou `UNCERTAIN`, junto com confiança e versão do modelo.
4. A requisição aguarda o resultado; somente `FOOD` aprovado continua para persistência.
5. `UNCERTAIN` e falhas do modelo devem resultar em erro controlado, sem publicar a imagem.
6. A imagem aprovada é normalizada para WebP, preservando no máximo 4K, e seus metadados são persistidos.

Critérios de aceite:

- Não publicar nem expor a imagem antes da validação.
- Definir timeout, limite de memória e comportamento quando o processo Python estiver indisponível.
- Registrar provider, versão do modelo, confiança, status e tentativa em `publication_image_checks`.
- Testar imagens válidas, não-imagens, imagens corrompidas, conteúdo não alimentar e resultado incerto.
- Manter o classificador substituível para permitir uma API externa no futuro.

### Tarefa 2 — Endurecer segurança de entradas, arquivos e respostas

Fazer uma revisão de segurança ponta a ponta, com prioridade para upload e publicação de imagens.

Escopo mínimo:

1. Limitar tamanho do request e do arquivo antes de gravar em disco.
2. Validar assinatura/magic bytes, não confiar em `Content-Type` ou extensão.
3. Bloquear path traversal, nomes controlados pelo usuário, links simbólicos e arquivos temporários públicos.
4. Impedir decompression bombs, dimensões abusivas, formatos inesperados e consumo excessivo de memória.
5. Remover metadados EXIF desnecessários e gerar nome UUID no armazenamento.
6. Evitar SSRF nos fluxos que aceitam URL de imagem, permitindo somente esquemas e destinos explicitamente seguros.
7. Garantir autorização por usuário em criação, edição, exclusão, imagens e moderação.
8. Padronizar respostas de erro sem stack trace, caminho local, credenciais ou detalhes internos.
9. Adicionar limites de requisição, logs sem dados sensíveis, headers de segurança e auditoria de ações administrativas.
10. Revisar dependências e criar testes de segurança para uploads malformados, arquivos poliglotas e payloads grandes.

Critérios de aceite:

- Arquivos inválidos são rejeitados antes de entrar no armazenamento público.
- Nenhum endpoint devolve conteúdo interno ou informação sensível em caso de erro.
- Upload, leitura, alteração e exclusão possuem testes de autorização.
- A configuração de limites fica explícita e ajustável por ambiente, sem colocar segredos no repositório.
- O fluxo síncrono possui timeout e não permite que uma validação indisponível publique conteúdo sem análise.

---

**Responsáveis:** Fagner e ChatGPT  
**Estado do handoff:** pronto para continuar no Codex dentro do IntelliJ.
