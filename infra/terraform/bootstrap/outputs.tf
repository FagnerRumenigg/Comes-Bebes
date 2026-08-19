output "resource_group_name" {
  value = azurerm_resource_group.tfstate.name
}

output "storage_account_name" {
  value = azurerm_storage_account.tfstate.name
}

output "container_name" {
  value = azurerm_storage_container.tfstate.name
}

output "backend_config_snippet" {
  description = "Cole isso (ou equivalente) no bloco backend \"azurerm\" do modulo principal (infra/terraform/main)."
  value = <<-EOT
    resource_group_name  = "${azurerm_resource_group.tfstate.name}"
    storage_account_name = "${azurerm_storage_account.tfstate.name}"
    container_name        = "${azurerm_storage_container.tfstate.name}"
    key                   = "comesebebes.tfstate"
  EOT
}
