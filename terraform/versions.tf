terraform {
  required_version = "= 1.16.5"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "= 5.8.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "= 3.9.1"
    }
  }

  # State lives in an Azure Storage container, so the deploy workflow and a laptop share it.
  # The names are passed at init time: terraform init -backend-config=...
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}
