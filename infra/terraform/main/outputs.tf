output "resource_group_name" {
  value = azurerm_resource_group.app.name
}

output "postgres_fqdn" {
  value = azurerm_postgresql_flexible_server.main.fqdn
}

output "container_registry_login_server" {
  value = azurerm_container_registry.acr.login_server
}

output "identity_client_id" {
  description = "Identidade de runtime da aplicação (Blob + AcrPull) — não é a de CI/CD."
  value       = azurerm_user_assigned_identity.app.client_id
}

output "identity_principal_id" {
  value = azurerm_user_assigned_identity.app.principal_id
}

output "cicd_identity_client_id" {
  description = "Client ID pro input client-id do azure/login@v2 nos workflows do GitHub Actions (não é secret — federação OIDC não usa client secret)."
  value       = azurerm_user_assigned_identity.cicd.client_id
}

output "cicd_tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}

output "cicd_subscription_id" {
  value = data.azurerm_client_config.current.subscription_id
}

output "api_fqdn" {
  description = "URL pública da API — vira o VITE_API_BASE_URL do frontend."
  value       = azurerm_container_app.api.ingress[0].fqdn
}

output "validator_internal_fqdn" {
  value = "${azurerm_container_app.validator.name}.internal.${azurerm_container_app_environment.main.default_domain}"
}

output "storage_account_images_name" {
  value = azurerm_storage_account.images.name
}

output "static_web_app_default_hostname" {
  description = "Domínio padrão *.azurestaticapps.net — continua ativo em paralelo ao domínio próprio (custom_domain_name), que é quem entra em COMESEBEBES_CORS_ALLOWED_ORIGINS/WEBAUTHN_RP_ID como origem principal (resolvido em main.tf)."
  value       = azurerm_static_web_app.frontend.default_host_name
}

output "static_web_app_deployment_token" {
  description = "Token de deploy do Static Web App — vira o secret AZURE_STATIC_WEB_APPS_API_KEY no GitHub Actions (Settings → Secrets → Actions). Sensível: não aparece em `terraform output` sem -raw, não vai pro log do CI/CD."
  value       = azurerm_static_web_app.frontend.api_key
  sensitive   = true
}

output "dns_zone_name_servers" {
  description = "Nameservers da zona Azure DNS de custom_domain_name. Passo manual único: colar esses 4 valores no painel do Registro.br (Meus Domínios → o domínio → DNS → usar servidores DNS diferentes). Sem isso, comesibebes.com.br não resolve pra nada."
  value       = azurerm_dns_zone.app.name_servers
}
