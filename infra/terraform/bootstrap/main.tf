# Bootstrap: cria só o necessário para o Terraform state remoto do módulo
# principal (infra/terraform/main). Não cria nenhum recurso da aplicação
# (Postgres, Blob de imagens, Container Apps etc.) — isso fica no módulo
# principal, que passa a usar o Storage Account criado aqui como backend.

resource "azurerm_resource_group" "tfstate" {
  name     = var.resource_group_name
  location = var.location
}

resource "azurerm_storage_account" "tfstate" {
  name                     = var.storage_account_name
  resource_group_name      = azurerm_resource_group.tfstate.name
  location                 = azurerm_resource_group.tfstate.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  # Nunca acessado pelo navegador; sem motivo pra permitir acesso público
  # a blob individual.
  allow_nested_items_to_be_public = false

  blob_properties {
    versioning_enabled = true

    delete_retention_policy {
      days = 30
    }
  }
}

resource "azurerm_storage_container" "tfstate" {
  name                  = var.container_name
  storage_account_id    = azurerm_storage_account.tfstate.id
  container_access_type = "private"
}
