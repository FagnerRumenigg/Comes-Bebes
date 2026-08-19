terraform {
  required_version = ">= 1.8.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Backend criado pelo módulo infra/terraform/bootstrap (já aplicado).
  # Valores conferem com os outputs desse módulo — ver
  # infra/terraform/bootstrap/outputs.tf.
  backend "azurerm" {
    resource_group_name  = "rg-comesebebes-tfstate"
    storage_account_name = "stcomesebebestfstate"
    container_name       = "tfstate"
    key                  = "comesebebes.tfstate"
  }
}

provider "azurerm" {
  features {}
}
