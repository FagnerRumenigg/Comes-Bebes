# ComeSebebes

Backend da rede social de comida ComeSebebes.

## Requisitos locais

- JDK 25.
- Maven 3.9 ou superior.
- Docker Desktop com a `Infra-Geral` em execução.
- Database e usuário exclusivos provisionados para o Comes & Bebes.

A aplicação não sobe o PostgreSQL. O Compose próprio em `../infra/compose.yml` sobe apenas a API e o validador de imagens.

## Configuração do banco

A aplicação usa um banco exclusivo dentro da instância PostgreSQL existente:

```text
database: comesebebes
schemas:
  - application: tabelas da aplicação
  - flyway: histórico das migrations
```

O usuário configurado em `DB_USERNAME` deve ser exclusivo desta aplicação. O Flyway cria `application` e `flyway` automaticamente e mantém seu histórico em `flyway`.

Crie o database manualmente, conectado à instância PostgreSQL:

```sql
CREATE DATABASE comesebebes;
```

Se o usuário não puder criar schemas, crie-os conectado ao database `comesebebes`:

```sql
CREATE SCHEMA application;
CREATE SCHEMA flyway;
```

## Variáveis de ambiente

O arquivo local de configuração fica em `../infra/.env` e nunca deve ser versionado:

```text
DB_HOST=platform-postgres
DB_PORT=5432
DB_NAME=comesebebes
DB_USERNAME=comesebebes_app
DB_PASSWORD=senha_exclusiva_da_aplicacao
API_HOST_PORT=8082
JWT_SECRET=segredo-jwt-com-no-minimo-32-caracteres
USER_BLOCKED_USERNAME_HMAC_SECRET=segredo-hmac-diferente-do-jwt
COMESEBEBES_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

As configurações de issuer, expiração de tokens e limite de login possuem valores padrão no `application.yml` e só precisam ser adicionadas ao ambiente se forem alteradas.

As variáveis dependentes do ambiente devem ser configuradas no `.env` da aplicação. Não coloque credenciais no Git ou neste README.

## Executar pelo IntelliJ IDEA

1. Abra o projeto `comesebebes` no IntelliJ IDEA.
2. Configure o JDK 25 para o projeto e para o Maven.
3. Crie ou edite uma configuração de execução para `org.application.Main`.
4. Adicione todas as variáveis de ambiente obrigatórias, usando os valores de `../infra/.env` e trocando `DB_HOST` por `localhost` quando executar fora do Docker.
5. Execute a classe `Main`.
6. Aguarde o Flyway concluir as migrations.

Exemplo de configuração de ambiente no IntelliJ:

```text
DB_HOST=localhost;DB_PORT=5432;DB_NAME=comesebebes;DB_USERNAME=comesebebes_app;DB_PASSWORD=senha_do_banco;COMESEBEBES_SERVER_PORT=8082;JWT_SECRET=uma-chave-local-com-pelo-menos-32-bytes;USER_BLOCKED_USERNAME_HMAC_SECRET=uma-chave-hmac-local;COMESEBEBES_IMAGE_STORAGE_PATH=./uploads/images;COMESEBEBES_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Executar pelo Maven

No PowerShell, defina as variáveis na sessão atual:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "comesebebes"
$env:DB_USERNAME = "comesebebes_app"
$env:DB_PASSWORD = "senha_do_banco"
$env:COMESEBEBES_SERVER_PORT = "8082"
```

Depois execute:

```powershell
mvn spring-boot:run
```

## Executar com Docker

Primeiro suba o repositório `Infra-Geral`. Depois, na raiz deste repositório, suba somente os serviços do Comes & Bebes:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml up --build -d
```

Verifique o estado e os logs:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml ps
docker compose --env-file .\infra\.env -f .\infra\compose.yml logs -f api validator
```

A API fica disponível diretamente em `http://localhost:8082` e pelo gateway em `http://localhost:8090/comesebebes/`. O validador fica acessível somente pela rede privada da aplicação.

Para parar somente o Comes & Bebes:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml down
```

Esse comando não derruba o PostgreSQL, o gateway nem as outras aplicações.

## Migrations

As migrations ficam em:

```text
src/main/resources/db/migration
```

O Flyway executa as migrations automaticamente na inicialização. As tabelas ficam em `application` e o histórico fica em `flyway`. O Hibernate usa `ddl-auto: validate`, portanto não cria nem altera tabelas.

## Swagger

Com a aplicação em execução, abra:

```text
http://localhost:8082/swagger-ui.html
```

A especificação OpenAPI fica disponível em:

```text
http://localhost:8082/v3/api-docs
```

O Swagger UI inicia com as controllers recolhidas. Os endpoints de autenticação são:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

Os endpoints de usuário já documentados são:

- `GET /users/{id}`
- `PATCH /users/{id}`
- `DELETE /users/{id}`

O endpoint `POST /auth/login` emite um token Bearer e um refresh token opaco. Após o login, clique em **Authorize** no Swagger e informe somente o JWT, sem escrever `Bearer` manualmente; a interface adiciona esse prefixo. Use `POST /auth/refresh` para renovar a sessão; a rotação revoga o refresh token anterior. Use `POST /auth/logout` para revogá-lo sem emitir uma nova sessão.

O cadastro e o login usam somente `username` e senha. O campo de e-mail permanece no banco, mas não é aceito nem retornado pela API no MVP. Usuários autenticados podem alterar a senha em `PATCH /users/{id}/password`, informando `currentPassword` e `newPassword`.

As imagens são baixadas para `COMESEBEBES_IMAGE_STORAGE_PATH`. No desenvolvimento local, a pasta `uploads/images` é criada automaticamente. O banco guarda somente os metadados do arquivo.

O login limita tentativas por IP e username. O limite local padrão é de 5 tentativas em 300 segundos e retorna `429` com o código `RATE_LIMIT_EXCEEDED`.

Erros possuem `status`, `code` e `message`. O frontend deve tratar o `code`, não o texto da mensagem.

## Problemas comuns

- `Could not resolve placeholder`: alguma variável de ambiente obrigatória não foi configurada.
- Falha de conexão: confira URL, porta, banco, usuário e senha.
- Falha ao criar schemas: crie `application` e `flyway` manualmente ou conceda permissão ao usuário.
- Falha de validação do Hibernate: confira se o Flyway executou todas as migrations no schema correto.
