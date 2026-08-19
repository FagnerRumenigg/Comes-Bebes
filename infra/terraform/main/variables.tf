# --- Geral ---------------------------------------------------------------

variable "location" {
  description = "Região Azure para todos os recursos da aplicação. Confirmada via Retail Prices API que suporta Container Apps, Postgres Flexible Server, Storage e Container Registry."
  type        = string
  default     = "brazilsouth"
}

variable "resource_group_name" {
  description = "Resource Group da aplicação (separado do Resource Group do Terraform state, criado no bootstrap)."
  type        = string
  default     = "rg-comesebebes"
}

# --- CI/CD (GitHub Actions) ---------------------------------------------

variable "github_repository" {
  description = "owner/repo do GitHub, usado no subject da federação OIDC."
  type        = string
  default     = "FagnerRumenigg/Comes-Bebes"
}

variable "github_environment_name" {
  description = "GitHub Environment que os workflows de release/deploy declaram — o subject da credencial federada é escopado a ele (não a uma tag específica, já que tags mudam a cada versão)."
  type        = string
  default     = "production"
}

variable "cicd_identity_name" {
  type    = string
  default = "id-comesebebes-cicd"
}

# --- Storage (imagens) -----------------------------------------------------

variable "storage_account_images_name" {
  description = "Nome da Storage Account que guarda as imagens aprovadas. Globalmente único, 3-24 caracteres, só minúsculas/números."
  type        = string
  default     = "stcomesebebesimages"
}

variable "storage_container_images_name" {
  description = "Container privado único de imagens aprovadas (sem quarentena, ver decisão 3.1 do plano v4)."
  type        = string
  default     = "comesebebes-images"
}

# --- PostgreSQL --------------------------------------------------------

variable "postgres_server_name" {
  description = "Nome do Azure Database for PostgreSQL Flexible Server. Globalmente único (vira parte do FQDN)."
  type        = string
  default     = "psql-comesebebes"
}

variable "postgres_database_name" {
  description = "Nome do database dentro do Flexible Server."
  type        = string
  default     = "comesebebes"
}

variable "postgres_version" {
  description = "Versão major do PostgreSQL. Confirmada igual à do Postgres local (platform-postgres-1, 17.10) antes da migração de dados."
  type        = string
  default     = "17"
}

variable "postgres_sku_name" {
  description = "SKU do Flexible Server. B_Standard_B1ms é o tier Burstable mais barato — adequado para ~50 usuários."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  description = "Storage do Flexible Server em MB. 32768 (32GB) é o mínimo permitido."
  type        = number
  default     = 32768
}

variable "postgres_backup_retention_days" {
  description = "Dias de retenção de backup automático (mínimo permitido pela Azure é 7)."
  type        = number
  default     = 7
}

variable "db_admin_username" {
  description = "Login administrador do Flexible Server. Também usado como usuário de conexão da aplicação nesta versão — simplificação deliberada (sem usuário de app separado) dado o estágio do projeto; evoluir se surgir necessidade real de separação de privilégios."
  type        = string
  default     = "comesebebes_admin"
}

variable "db_admin_password" {
  description = "Senha do administrador do Flexible Server. Sem default — precisa ser fornecida no apply (-var ou .tfvars fora do Git)."
  type        = string
  sensitive   = true
}

# --- Identidade e Container Registry ---------------------------------------

variable "identity_name" {
  description = "User Assigned Managed Identity compartilhada pelos dois Container Apps (API e validador) — acesso a Blob e ACR sem chave/senha guardada."
  type        = string
  default     = "id-comesebebes-app"
}

variable "container_registry_name" {
  description = "Nome do Azure Container Registry. Globalmente único, só alfanumérico."
  type        = string
  default     = "acrcomesebebes"
}

variable "container_registry_sku" {
  description = "SKU do ACR. Standard porque o benefício de 12 meses grátis da conta Azure cobre especificamente 1 registry Standard (100GB, 10 webhooks) — Basic sairia mais caro nesse período por não estar coberto pelo benefício. Reavaliar antes de 2027-08-18: sem o benefício, Standard custa ~US$20/mês contra ~US$5/mês do Basic; rebaixar pra Basic nessa data se o uso não justificar Standard."
  type        = string
  default     = "Standard"
}

# --- Container Apps Environment ---------------------------------------

variable "log_analytics_workspace_name" {
  type    = string
  default = "log-comesebebes"
}

variable "log_analytics_retention_days" {
  type    = number
  default = 30
}

variable "container_app_environment_name" {
  type    = string
  default = "cae-comesebebes"
}

# --- Imagem placeholder ------------------------------------------------

variable "placeholder_image" {
  description = "Imagem pública usada só para o primeiro apply, antes do CI/CD publicar a imagem real no ACR (evita o problema de referenciar uma imagem que ainda não existe). O Terraform ignora mudanças nesse campo depois (ver lifecycle nos Container Apps) — quem passa a controlar a tag em produção é o pipeline de deploy, não o Terraform."
  type        = string
  default     = "mcr.microsoft.com/k8se/quickstart:latest"
}

# --- Container App: API -------------------------------------------------

variable "api_app_name" {
  type    = string
  default = "ca-comesebebes-api"
}

variable "api_target_port" {
  description = "Porta interna da API (COMESEBEBES_SERVER_PORT, igual ao infra/compose.yml local)."
  type        = number
  default     = 8082
}

variable "api_cpu" {
  type    = number
  default = 0.5
}

variable "api_memory" {
  type    = string
  default = "1Gi"
}

variable "api_min_replicas" {
  description = "0 — decisão pra fase de testes (custo baixo, cold start ~1-2min aceitável enquanto só há uso esporádico). Rate limiters agora persistem em Postgres (RateLimitStore), não é mais um bloqueio técnico pra escalar a zero — só max_replicas=1 continua obrigatório, pra nunca ter 2 réplicas simultâneas."
  type        = number
  default     = 0
}

variable "api_max_replicas" {
  type    = number
  default = 1
}

# --- Container App: validador -------------------------------------------

variable "validator_app_name" {
  type    = string
  default = "ca-comesebebes-validator"
}

variable "validator_target_port" {
  description = "Porta interna do validador (definida no Dockerfile: EXPOSE 8001)."
  type        = number
  default     = 8001
}

variable "validator_cpu" {
  description = "1 vCPU — folga sobre o pico de ~637MB de RAM medido em Validator_Runtime_Analysis.md."
  type        = number
  default     = 1.0
}

variable "validator_memory" {
  type    = string
  default = "2Gi"
}

variable "validator_min_replicas" {
  description = "0 — decidido por custo real via Retail Prices API (centavos/mês vs. ~US$23/mês em min=1), cold start ~3,5-4s aceitável nesta escala."
  type        = number
  default     = 0
}

variable "validator_max_replicas" {
  type    = number
  default = 3
}

# --- Config da aplicação (não-segredo) ----------------------------------

variable "cors_allowed_origins" {
  type    = string
  default = "http://localhost:3000,http://localhost:5173,https://fagnerrumenigg.github.io"
}

variable "webauthn_rp_id" {
  description = "Domínio do frontend (GitHub Pages) — não muda com a migração."
  type        = string
  default     = "fagnerrumenigg.github.io"
}

variable "jwt_issuer" {
  type    = string
  default = "comesebebes"
}

variable "jwt_expiration_minutes" {
  type    = number
  default = 60
}

variable "refresh_token_expiration_days" {
  type    = number
  default = 30
}

variable "login_rate_limit_max_attempts" {
  type    = number
  default = 5
}

variable "login_rate_limit_window_seconds" {
  type    = number
  default = 300
}

variable "publication_rate_limit_max_attempts" {
  type    = number
  default = 3
}

variable "publication_rate_limit_window_seconds" {
  type    = number
  default = 600
}

variable "validator_connect_timeout_ms" {
  type    = number
  default = 5000
}

variable "validator_read_timeout_ms" {
  description = "Generoso o bastante pra cobrir um cold start do validador com minReplicas=0 (~3,5-4s medido) com folga."
  type        = number
  default     = 20000
}

# --- Segredos (sem default — obrigatório fornecer no apply) ----------------

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "user_blocked_username_hmac_secret" {
  type      = string
  sensitive = true
}

# --- Orçamento / alerta de custo ----------------------------------------

variable "monthly_budget_amount" {
  description = "Teto mensal de referência pro alerta de custo, na moeda de faturamento real da conta (BRL nesta assinatura — o recurso azurerm_consumption_budget_resource_group não aceita moeda explícita, usa a do billing account). Ajustável — não é um limite técnico, só dispara notificação. R$25 é abaixo do teto estimado com min=1 na API (~R$178-190/mês), mas com min=0 na API + validador o gasto real esperado é bem menor — o alerta serve pra avisar cedo se passar disso."
  type        = number
  default     = 25
}

variable "budget_alert_email" {
  description = "E-mail que recebe o alerta de orçamento."
  type        = string
  default     = "rumeniggmoraes@gmail.com"
}

variable "budget_start_date" {
  type    = string
  default = "2026-08-01T00:00:00Z"
}

variable "budget_end_date" {
  type    = string
  default = "2027-08-01T00:00:00Z"
}
