data "azurerm_client_config" "current" {}

resource "azurerm_resource_group" "app" {
  name     = var.resource_group_name
  location = var.location
}

# --- Identidade compartilhada (API + validador) ----------------------------
# Usada pra acessar o Blob de imagens (só pela API) e puxar imagem do ACR
# (pelos dois) — sem chave/senha guardada em lugar nenhum.

resource "azurerm_user_assigned_identity" "app" {
  name                = var.identity_name
  resource_group_name = azurerm_resource_group.app.name
  location            = azurerm_resource_group.app.location
}

# --- Storage: imagens aprovadas --------------------------------------------
# Um único container privado, sem quarentena — nada é gravado antes de
# aprovado pelo validador (decisão 3.1 do plano v4).

resource "azurerm_storage_account" "images" {
  name                     = var.storage_account_images_name
  resource_group_name      = azurerm_resource_group.app.name
  location                 = azurerm_resource_group.app.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  allow_nested_items_to_be_public = false

  blob_properties {
    versioning_enabled = true

    delete_retention_policy {
      days = 30
    }
  }
}

resource "azurerm_storage_container" "images" {
  name                  = var.storage_container_images_name
  storage_account_id    = azurerm_storage_account.images.id
  container_access_type = "private"
}

resource "azurerm_role_assignment" "app_blob_data_contributor" {
  scope                = azurerm_storage_account.images.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.app.principal_id
}

# --- PostgreSQL Flexible Server ---------------------------------------------
# Sem VNet/subnet delegada (mantém simples, sem componente extra) — acesso
# público restrito à regra "Allow Azure services", que é o padrão pra
# Container Apps sem integração de VNet nesta escala do projeto.
#
# Dimensionamento (B1MS, 32GB storage) escolhido de propósito pra caber
# inteiro no benefício de 12 meses grátis da conta Azure (750h de B1MS +
# 32GB storage + 32GB backup) — confirmado em
# https://azure.microsoft.com/pricing/purchase-options/azure-account/.
# Passa a ser cobrado (~US$32/mês) só depois de 2027-08-18 ou se o storage
# crescer além de 32GB.

resource "azurerm_postgresql_flexible_server" "main" {
  name                = var.postgres_server_name
  resource_group_name = azurerm_resource_group.app.name
  location            = azurerm_resource_group.app.location

  version = var.postgres_version

  administrator_login    = var.db_admin_username
  administrator_password = var.db_admin_password

  storage_mb                   = var.postgres_storage_mb
  sku_name                     = var.postgres_sku_name
  backup_retention_days        = var.postgres_backup_retention_days
  geo_redundant_backup_enabled = false

  public_network_access_enabled = true

  # Azure atribui a zona automaticamente na criação; não deixamos o Terraform
  # gerenciar esse campo (tentar zerá-lo depois falha: "zone can only be
  # changed when exchanged with high_availability.standby_availability_zone").
  lifecycle {
    ignore_changes = [zone]
  }
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = var.postgres_database_name
  server_id = azurerm_postgresql_flexible_server.main.id
  collation = "en_US.utf8"
  charset   = "UTF8"

  lifecycle {
    prevent_destroy = true
  }
}

# --- Container Registry -----------------------------------------------

resource "azurerm_container_registry" "acr" {
  name                = var.container_registry_name
  resource_group_name = azurerm_resource_group.app.name
  location            = azurerm_resource_group.app.location
  sku                 = var.container_registry_sku
  admin_enabled       = false
}

resource "azurerm_role_assignment" "app_acr_pull" {
  scope                            = azurerm_container_registry.acr.id
  role_definition_name             = "AcrPull"
  principal_id                     = azurerm_user_assigned_identity.app.principal_id
  skip_service_principal_aad_check = true
}

# --- Identidade de CI/CD (GitHub Actions) --------------------------------
# Separada da identidade de runtime da app (azurerm_user_assigned_identity.app):
# só essa identidade pode publicar imagem no ACR e atualizar os Container Apps;
# a identidade de runtime só tem AcrPull + acesso ao Blob. Federação OIDC —
# sem client secret armazenado em lugar nenhum (nem GitHub, nem Terraform state).

resource "azurerm_user_assigned_identity" "cicd" {
  name                = var.cicd_identity_name
  resource_group_name = azurerm_resource_group.app.name
  location            = azurerm_resource_group.app.location
}

resource "azurerm_federated_identity_credential" "cicd_github" {
  name                      = "github-actions-${var.github_environment_name}"
  user_assigned_identity_id = azurerm_user_assigned_identity.cicd.id
  issuer                    = "https://token.actions.githubusercontent.com"
  audience                  = ["api://AzureADTokenExchange"]
  # Escopado ao GitHub Environment, não a uma tag específica — tags mudam a
  # cada release (api-v1.9.1, api-v1.9.2, ...) e a Azure não aceita subject
  # com wildcard. Os workflows de release/deploy precisam declarar
  # `environment: ${var.github_environment_name}` pra esse subject bater.
  # Formato imutável "owner@owner_id/repo@repo_id" desde 2026-04-23 (ver
  # variables.tf) — sem os IDs a Azure rejeita com AADSTS700213.
  subject = "repo:${split("/", var.github_repository)[0]}@${var.github_owner_id}/${split("/", var.github_repository)[1]}@${var.github_repo_id}:environment:${var.github_environment_name}"
}

resource "azurerm_role_assignment" "cicd_acr_push" {
  scope                            = azurerm_container_registry.acr.id
  role_definition_name             = "AcrPush"
  principal_id                     = azurerm_user_assigned_identity.cicd.principal_id
  skip_service_principal_aad_check = true
}

resource "azurerm_role_assignment" "cicd_container_apps_contributor" {
  scope                            = azurerm_resource_group.app.id
  role_definition_name             = "Container Apps Contributor"
  principal_id                     = azurerm_user_assigned_identity.cicd.principal_id
  skip_service_principal_aad_check = true
}

# --- Container Apps Environment ---------------------------------------

resource "azurerm_log_analytics_workspace" "main" {
  name                = var.log_analytics_workspace_name
  resource_group_name = azurerm_resource_group.app.name
  location            = azurerm_resource_group.app.location
  sku                 = "PerGB2018"
  retention_in_days   = var.log_analytics_retention_days
}

resource "azurerm_container_app_environment" "main" {
  name                       = var.container_app_environment_name
  resource_group_name        = azurerm_resource_group.app.name
  location                   = azurerm_resource_group.app.location
  logs_destination           = "log-analytics"
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  # Azure cria um workload_profile "Consumption" padrão sozinho; não deixamos
  # o Terraform tentar removê-lo (drift cosmético, sem efeito real).
  lifecycle {
    ignore_changes = [workload_profile]
  }
}

# --- Container App: validador -------------------------------------------
# Só acessível de dentro do Environment (ingress interno) — a API é a única
# chamadora. minReplicas=0: stateless, custo em repouso é ~zero (ver
# Validator_Runtime_Analysis.md / Investigacao_Validator_Cold_Start.md).

resource "azurerm_container_app" "validator" {
  name                         = var.validator_app_name
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.app.name
  revision_mode                = "Single"

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.app.id]
  }

  registry {
    server   = azurerm_container_registry.acr.login_server
    identity = azurerm_user_assigned_identity.app.id
  }

  ingress {
    external_enabled           = false
    target_port                = var.validator_target_port
    allow_insecure_connections = true
    transport                  = "http"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = var.validator_min_replicas
    max_replicas = var.validator_max_replicas

    http_scale_rule {
      name                = "http-scaler"
      concurrent_requests = "10"
    }

    container {
      name   = "validator"
      image  = var.placeholder_image
      cpu    = var.validator_cpu
      memory = var.validator_memory
    }
  }

  # A tag da imagem é controlada pelo CI/CD (deploy-server.yml) depois do
  # primeiro apply, não pelo Terraform — evita o Terraform reverter o
  # deploy mais recente a cada `plan`/`apply` de infraestrutura.
  lifecycle {
    ignore_changes = [template[0].container[0].image, workload_profile_name]
  }

  depends_on = [azurerm_role_assignment.app_acr_pull]
}

# --- Container App: API -------------------------------------------------
# max=1 continua fixo pra nunca ter 2 réplicas simultâneas. min=0 desde
# 2026-08-25 (ver variables.tf, api_min_replicas) — os rate limiters já
# persistem em Postgres (RateLimitStore), não são mais o bloqueio técnico
# pra escalar a zero.

resource "azurerm_container_app" "api" {
  name                         = var.api_app_name
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.app.name
  revision_mode                = "Single"

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.app.id]
  }

  registry {
    server   = azurerm_container_registry.acr.login_server
    identity = azurerm_user_assigned_identity.app.id
  }

  secret {
    name  = "db-password"
    value = var.db_admin_password
  }

  secret {
    name  = "jwt-secret"
    value = var.jwt_secret
  }

  secret {
    name  = "user-blocked-username-hmac-secret"
    value = var.user_blocked_username_hmac_secret
  }

  secret {
    name  = "email-azure-access-key"
    value = var.email_azure_access_key
  }

  ingress {
    external_enabled = true
    target_port      = var.api_target_port
    transport        = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = var.api_min_replicas
    max_replicas = var.api_max_replicas

    http_scale_rule {
      name                = "http-scaler"
      concurrent_requests = "10"
    }

    container {
      name   = "api"
      image  = var.placeholder_image
      cpu    = var.api_cpu
      memory = var.api_memory

      env {
        name  = "COMESEBEBES_SERVER_PORT"
        value = tostring(var.api_target_port)
      }
      env {
        name  = "DB_HOST"
        value = azurerm_postgresql_flexible_server.main.fqdn
      }
      env {
        name  = "DB_PORT"
        value = "5432"
      }
      env {
        name  = "DB_NAME"
        value = var.postgres_database_name
      }
      env {
        name  = "DB_URL_PARAMS"
        value = "?sslmode=require"
      }
      env {
        name  = "DB_USERNAME"
        value = var.db_admin_username
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      env {
        name        = "JWT_SECRET"
        secret_name = "jwt-secret"
      }
      env {
        name        = "USER_BLOCKED_USERNAME_HMAC_SECRET"
        secret_name = "user-blocked-username-hmac-secret"
      }
      env {
        name  = "COMESEBEBES_STORAGE_TYPE"
        value = "blob"
      }
      env {
        name  = "COMESEBEBES_STORAGE_BLOB_ACCOUNT_URL"
        value = "https://${azurerm_storage_account.images.name}.blob.core.windows.net"
      }
      env {
        name  = "COMESEBEBES_STORAGE_BLOB_CONTAINER"
        value = var.storage_container_images_name
      }
      env {
        name  = "COMESEBEBES_STORAGE_BLOB_IDENTITY_CLIENT_ID"
        value = azurerm_user_assigned_identity.app.client_id
      }
      env {
        name  = "COMESEBEBES_IMAGE_VALIDATOR_URL"
        value = "http://${azurerm_container_app.validator.name}.internal.${azurerm_container_app_environment.main.default_domain}"
      }
      env {
        name  = "COMESEBEBES_IMAGE_VALIDATOR_CONNECT_TIMEOUT_MS"
        value = tostring(var.validator_connect_timeout_ms)
      }
      env {
        name  = "COMESEBEBES_IMAGE_VALIDATOR_READ_TIMEOUT_MS"
        value = tostring(var.validator_read_timeout_ms)
      }
      env {
        name  = "COMESEBEBES_CORS_ALLOWED_ORIGINS"
        value = "${var.cors_allowed_origins},https://${var.custom_domain_name},https://www.${var.custom_domain_name},https://${azurerm_static_web_app.frontend.default_host_name}"
      }
      env {
        name  = "COMESEBEBES_WEBAUTHN_RP_ID"
        value = var.custom_domain_name
      }
      env {
        name  = "COMESEBEBES_JWT_ISSUER"
        value = var.jwt_issuer
      }
      env {
        name  = "COMESEBEBES_JWT_EXPIRATION_MINUTES"
        value = tostring(var.jwt_expiration_minutes)
      }
      env {
        name  = "COMESEBEBES_REFRESH_TOKEN_EXPIRATION_DAYS"
        value = tostring(var.refresh_token_expiration_days)
      }
      env {
        name  = "COMESEBEBES_LOGIN_RATE_LIMIT_MAX_ATTEMPTS"
        value = tostring(var.login_rate_limit_max_attempts)
      }
      env {
        name  = "COMESEBEBES_LOGIN_RATE_LIMIT_WINDOW_SECONDS"
        value = tostring(var.login_rate_limit_window_seconds)
      }
      env {
        name  = "COMESEBEBES_PUBLICATION_RATE_LIMIT_MAX_ATTEMPTS"
        value = tostring(var.publication_rate_limit_max_attempts)
      }
      env {
        name  = "COMESEBEBES_PUBLICATION_RATE_LIMIT_WINDOW_SECONDS"
        value = tostring(var.publication_rate_limit_window_seconds)
      }
      env {
        name  = "COMESEBEBES_FRONTEND_URL"
        value = "https://${var.custom_domain_name}"
      }
      env {
        name  = "COMESEBEBES_EMAIL_DELIVERY_MODE"
        value = var.email_delivery_mode
      }
      env {
        name  = "COMESEBEBES_EMAIL_SENDER_ADDRESS"
        value = var.email_sender_address
      }
      env {
        name  = "COMESEBEBES_EMAIL_AZURE_ENDPOINT"
        value = var.email_azure_endpoint
      }
      env {
        name        = "COMESEBEBES_EMAIL_AZURE_ACCESS_KEY"
        secret_name = "email-azure-access-key"
      }

      liveness_probe {
        transport = "HTTP"
        port      = var.api_target_port
        path      = "/actuator/health/liveness"
      }

      readiness_probe {
        transport = "HTTP"
        port      = var.api_target_port
        path      = "/actuator/health/readiness"
      }
    }
  }

  lifecycle {
    ignore_changes = [template[0].container[0].image, workload_profile_name]
  }

  depends_on = [
    azurerm_role_assignment.app_acr_pull,
    azurerm_role_assignment.app_blob_data_contributor,
    azurerm_postgresql_flexible_server_firewall_rule.allow_azure_services,
  ]
}

# --- Static Web App (frontend) -------------------------------------------
# Substitui o GitHub Pages. Tier Free (100GB/mês de banda, SSL grátis). O
# conteúdo em si é publicado pelo workflow do GitHub Actions
# (deploy-static-web-app.yml), não pelo Terraform — esse recurso só
# provisiona o "onde". Domínio próprio (comesibebes.com.br) é configurado
# logo abaixo; *.azurestaticapps.net continua ativo em paralelo.

resource "azurerm_static_web_app" "frontend" {
  name                = var.static_web_app_name
  resource_group_name = azurerm_resource_group.app.name
  location            = var.static_web_app_location
  sku_tier            = "Free"
  sku_size            = "Free"
}

# --- Domínio próprio (Azure DNS) ------------------------------------------
# comesibebes.com.br, comprado no Registro.br. DNS migrado pra Azure DNS
# (decisão 2026-09-10) pra dar pro Terraform gerenciar tudo — TXT de
# validação, ALIAS do apex e CNAME do www — num apply só. Passo manual
# único, fora do Terraform: trocar os nameservers do domínio no painel do
# Registro.br pelos gerados abaixo (output dns_zone_name_servers). Enquanto
# isso não for feito, nada aqui resolve publicamente.
#
# O apex usa validação dns-txt-token (obrigatória em domínio apex — CNAME
# não é permitido na raiz da zona) e roteia via ALIAS record (A record
# apontando pro recurso do Static Web App em si, não pra um IP fixo, senão
# perde a distribuição global). A validação do apex é assíncrona — o
# Terraform não espera ela terminar, então o primeiro apply passa mesmo
# sem os nameservers ainda propagados. Já o www usa cname-delegation, que é
# síncrona (o provider ativamente confere se o CNAME resolve) — só valida
# depois que a troca de nameservers tiver propagado; pode exigir um
# segundo apply.

resource "azurerm_dns_zone" "app" {
  name                = var.custom_domain_name
  resource_group_name = azurerm_resource_group.app.name
}

resource "azurerm_static_web_app_custom_domain" "apex" {
  static_web_app_id = azurerm_static_web_app.frontend.id
  domain_name       = var.custom_domain_name
  validation_type   = "dns-txt-token"
}

resource "azurerm_dns_txt_record" "apex_validation" {
  name                = "@"
  zone_name           = azurerm_dns_zone.app.name
  resource_group_name = azurerm_resource_group.app.name
  ttl                 = 300

  record {
    # validation_token só existe enquanto a validação está pendente —
    # depois que valida, a Azure zera o campo (vira ""). Sem o fallback,
    # a expressão vira string vazia e o provider rejeita antes mesmo de
    # calcular o diff (rejeita "" mesmo com ignore_changes abaixo, que só
    # evita a atualização em si, não a validação do valor). O registro já
    # cumpriu seu papel depois de validado, não precisa mais mudar.
    value = (azurerm_static_web_app_custom_domain.apex.validation_token == null || azurerm_static_web_app_custom_domain.apex.validation_token == "") ? "dominio-ja-validado" : azurerm_static_web_app_custom_domain.apex.validation_token
  }

  lifecycle {
    ignore_changes = [record]
  }
}

resource "azurerm_dns_a_record" "apex_alias" {
  name                = "@"
  zone_name           = azurerm_dns_zone.app.name
  resource_group_name = azurerm_resource_group.app.name
  ttl                 = 300
  target_resource_id  = azurerm_static_web_app.frontend.id

  depends_on = [azurerm_dns_txt_record.apex_validation]
}

resource "azurerm_dns_cname_record" "www" {
  name                = "www"
  zone_name           = azurerm_dns_zone.app.name
  resource_group_name = azurerm_resource_group.app.name
  ttl                 = 300
  record              = azurerm_static_web_app.frontend.default_host_name
}

resource "azurerm_static_web_app_custom_domain" "www" {
  static_web_app_id = azurerm_static_web_app.frontend.id
  domain_name       = "www.${var.custom_domain_name}"
  validation_type   = "cname-delegation"

  depends_on = [azurerm_dns_cname_record.www]
}

# --- Budget / alerta de custo --------------------------------------------

resource "azurerm_consumption_budget_resource_group" "app" {
  name              = "budget-comesebebes"
  resource_group_id = azurerm_resource_group.app.id

  amount     = var.monthly_budget_amount
  time_grain = "Monthly"

  time_period {
    start_date = var.budget_start_date
    end_date   = var.budget_end_date
  }

  notification {
    enabled        = true
    operator       = "GreaterThan"
    threshold      = 80
    threshold_type = "Actual"

    contact_emails = var.budget_alert_emails
  }

  notification {
    enabled        = true
    operator       = "GreaterThan"
    threshold      = 100
    threshold_type = "Forecasted"

    contact_emails = var.budget_alert_emails
  }
}
