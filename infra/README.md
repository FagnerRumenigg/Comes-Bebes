# Infraestrutura do Comes & Bebes

Este Compose pertence somente ao Comes & Bebes. Ele sobe a API Java e o validador Python, conectando-os às redes externas criadas pelo repositório `Infra-Geral`.

## Pré-requisitos

- A `Infra-Geral` deve estar em execução.
- As redes `platform-edge` e `platform-data` devem existir.
- O database `comesebebes` e o usuário `comesebebes_app` devem estar provisionados.

## Configuração

O arquivo real `infra/.env` é local e ignorado pelo Git. Para criá-lo a partir da credencial gerada pela Infra-Geral:

```powershell
.\infra\Initialize-Environment.ps1 -DatabaseCredentialFile "CAMINHO\comesebebes.env"
```

O script cria JWT e HMAC aleatórios sem exibi-los no terminal. Confira apenas os valores não secretos e ajuste `COMESEBEBES_CORS_ALLOWED_ORIGINS` quando o endereço do frontend estiver definido.

## Executar

A API e o validador não são mais buildados localmente: o `infra/compose.yml` consome imagens publicadas no Docker Hub (`fagnerrumenigg/comesebebes-api`, `fagnerrumenigg/comesebebes-validator`), versionadas por `API_VERSION`/`VALIDATOR_VERSION` no `infra/.env`.

Na raiz do repositório:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml pull
docker compose --env-file .\infra\.env -f .\infra\compose.yml up -d
docker compose --env-file .\infra\.env -f .\infra\compose.yml ps
```

A API fica disponível diretamente em `http://localhost:8082` e, pelo gateway, em `http://localhost:8090/comesebebes/`.

Para parar apenas esta aplicação:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml down
```

Esse comando não remove PostgreSQL, gateway, redes externas, imagens enviadas nem o cache nomeado do modelo. Não use `down -v` se quiser preservar o cache.

## Publicar uma nova versão

A API e o validador têm ciclos de release independentes, cada um disparado por uma Git tag anotada. Ver `docs/PLANEJAMENTO_v11.md` (MEL-007) para o desenho completo.

Antes de tudo, `main` precisa estar atualizada e sem alterações locais pendentes:

```powershell
git checkout main
git pull origin main
```

### API

```powershell
git tag -a api-v0.X.Y -m "API release 0.X.Y"
git push origin api-v0.X.Y
```

Isso dispara `.github/workflows/release-api.yml`: roda os testes Java, builda a imagem e publica `fagnerrumenigg/comesebebes-api:0.X.Y` no Docker Hub. O workflow do validador **não** é acionado.

### image-validator

```powershell
git tag -a validator-v0.X.Y -m "Validator release 0.X.Y"
git push origin validator-v0.X.Y
```

Isso dispara `.github/workflows/release-validator.yml` (só ele), publicando `fagnerrumenigg/comesebebes-validator:0.X.Y`.

### Atualizar o ambiente depois de uma release

Rodar, no servidor onde este Compose está em execução:

```powershell
.\infra\Deploy-Release.ps1 -Service api -Version 0.X.Y
```

(ou `-Service validator`). O script atualiza `API_VERSION`/`VALIDATOR_VERSION` em `infra/.env`, dá `pull` e `up -d` só do serviço informado.

Isso é equivalente a fazer manualmente:

```powershell
# editar infra/.env: API_VERSION=0.X.Y
docker compose --env-file .\infra\.env -f .\infra\compose.yml pull api
docker compose --env-file .\infra\.env -f .\infra\compose.yml up -d api
```

### Rollback

Trocar `API_VERSION`/`VALIDATOR_VERSION` de volta para uma versão anterior já publicada e repetir `pull`+`up -d` do serviço correspondente. Uma versão já publicada nunca deve ser sobrescrita — em caso de correção, publicar `0.X.(Y+1)` em vez de reutilizar a tag.

## Persistência

- Imagens: `backend/uploads/images` no host, montada em `/app/uploads/images`.
- Modelo do validador: volume Docker `comesebebes_validator_model_cache`.
- Dados relacionais: database `comesebebes` na `Infra-Geral`.

As imagens não se perdem ao reiniciar ou recriar o container porque ficam fora da camada gravável dele. O backup consistente da aplicação deve incluir o database e `backend/uploads/images`.
