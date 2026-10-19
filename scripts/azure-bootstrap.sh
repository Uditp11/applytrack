#!/usr/bin/env bash
# One-time setup, run by the owner of the Azure subscription on their own machine after "az login".
# Creates what Terraform itself needs before the first deploy:
#   1. a storage account for the Terraform state (costs cents at most; inside the free tier for new accounts)
#   2. an app registration that GitHub Actions can log in as with OpenID Connect (no password stored anywhere)
# Then it prints the values to save as GitHub repository secrets.
# Usage: scripts/azure-bootstrap.sh <github-owner>/<repository>   e.g. scripts/azure-bootstrap.sh Uditp11/applytrack
set -euo pipefail

repo=${1:?usage: scripts/azure-bootstrap.sh <github-owner>/<repository>}
location=${LOCATION:-germanywestcentral}
state_rg=applytrack-tfstate-rg
state_account=applytrackstate$(openssl rand -hex 3)

subscription=$(az account show --query id -o tsv)
tenant=$(az account show --query tenantId -o tsv)

# 1. Terraform state storage
az group create --name "$state_rg" --location "$location" --output none
az storage account create --name "$state_account" --resource-group "$state_rg" --location "$location" \
  --sku Standard_LRS --min-tls-version TLS1_2 --allow-blob-public-access false --output none
az storage container create --name tfstate --account-name "$state_account" --auth-mode login --output none

# 2. Identity for the deploy workflow, trusted only for this repository's manual runs on main
app_id=$(az ad app create --display-name applytrack-deploy --query appId -o tsv)
az ad sp create --id "$app_id" --output none
az ad app federated-credential create --id "$app_id" --parameters "{
  \"name\": \"github-main\",
  \"issuer\": \"https://token.actions.githubusercontent.com\",
  \"subject\": \"repo:$repo:ref:refs/heads/main\",
  \"audiences\": [\"api://AzureADTokenExchange\"]
}" --output none
az role assignment create --assignee "$app_id" --role Contributor --scope "/subscriptions/$subscription" --output none
az role assignment create --assignee "$app_id" --role "Storage Blob Data Contributor" \
  --scope "$(az storage account show --name "$state_account" --resource-group "$state_rg" --query id -o tsv)" --output none

cat <<OUT

Done. Add these as GitHub repository secrets (Settings > Secrets and variables > Actions):
  AZURE_CLIENT_ID           $app_id
  AZURE_TENANT_ID           $tenant
  AZURE_SUBSCRIPTION_ID     $subscription
  TF_STATE_RESOURCE_GROUP   $state_rg
  TF_STATE_STORAGE_ACCOUNT  $state_account
  BUDGET_ALERT_EMAIL        <your e-mail address>
OUT
