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

Na raiz do repositório:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml up --build -d
docker compose --env-file .\infra\.env -f .\infra\compose.yml ps
```

A API fica disponível diretamente em `http://localhost:8082` e, pelo gateway, em `http://localhost:8090/comesebebes/`.

Para parar apenas esta aplicação:

```powershell
docker compose --env-file .\infra\.env -f .\infra\compose.yml down
```

Esse comando não remove PostgreSQL, gateway, redes externas, imagens enviadas nem o cache nomeado do modelo. Não use `down -v` se quiser preservar o cache.

## Persistência

- Imagens: `backend/uploads/images` no host, montada em `/app/uploads/images`.
- Modelo do validador: volume Docker `comesebebes_validator_model_cache`.
- Dados relacionais: database `comesebebes` na `Infra-Geral`.

As imagens não se perdem ao reiniciar ou recriar o container porque ficam fora da camada gravável dele. O backup consistente da aplicação deve incluir o database e `backend/uploads/images`.
