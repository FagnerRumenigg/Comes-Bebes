variable "location" {
  description = "Regiao Azure para os recursos de bootstrap do Terraform state."
  type        = string
  default     = "brazilsouth"
}

variable "resource_group_name" {
  description = "Nome do Resource Group dedicado ao Terraform state remoto (separado do Resource Group da aplicacao)."
  type        = string
  default     = "rg-comesebebes-tfstate"
}

variable "storage_account_name" {
  description = "Nome da Storage Account dedicada ao Terraform state remoto. Globalmente unico em toda a Azure, 3-24 caracteres, so minusculas e numeros."
  type        = string
  default     = "stcomesebebestfstate"
}

variable "container_name" {
  description = "Nome do container de blobs onde os arquivos de state remoto ficam armazenados."
  type        = string
  default     = "tfstate"
}
