terraform {
  required_version = ">= 1.8.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Bootstrap propositalmente NAO usa backend remoto: ele e quem CRIA o
  # Storage Account que o modulo principal vai usar como backend. State
  # local aqui, nunca commitado (ver .gitignore).
}

provider "azurerm" {
  features {}
}
