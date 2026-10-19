#!/usr/bin/env bash
# One-time setup, run by the owner of the Azure subscription on their own machine after "az login".
# Creates what Terraform itself needs before the first deploy:
#   1. a storage account for the Terraform state (costs cents at most; inside the free tier for new accounts)
#   2. a managed identity that GitHub Actions can log in as with OpenID Connect (no password stored anywhere;
#      a managed identity also works where a student account may not register applications)
# Then it prints the values to save as GitHub repository secrets. Safe to run again: the names are fixed.
# Usage: scripts/azure-bootstrap.sh <github-owner>/<repository>   e.g. scripts/azure-bootstrap.sh Uditp11/applytrack
#        LOCATION=swedencentral scripts/azure-bootstrap.sh ...      to use another allowed region
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash on Windows: do not turn /subscriptions/... into a Windows path

repo=${1:?usage: scripts/azure-bootstrap.sh <github-owner>/<repository>   (exact case, as on GitHub)}
location=${LOCATION:-francecentral}   # allowed for Azure for Students, and offers PostgreSQL B1ms
state_rg=applytrack-tfstate-rg
identity=applytrack-deploy

# az on Windows ends its output lines with \r; strip it so the values can be used in later commands.
az_value() { az "$@" --output tsv | tr -d '\r'; }

subscription=$(az_value account show --query id)
tenant=$(az_value account show --query tenantId)
echo "Subscription: $(az_value account show --query name) ($subscription), region: $location"

# The services Terraform uses must be enabled once per subscription.
for provider in Microsoft.App Microsoft.DBforPostgreSQL Microsoft.Consumption Microsoft.Storage Microsoft.ManagedIdentity; do
  az provider register --namespace "$provider" --wait --output none
done

# Stop before creating anything if this subscription cannot use the free database size in this region.
if ! az_value postgres flexible-server list-skus --location "$location" \
    --query "[].supportedServerEditions[].supportedServerSkus[].name" | grep -qx 'Standard_B1ms'; then
  echo "PostgreSQL Standard_B1ms is not available to this subscription in $location. Try LOCATION=<another region>." >&2
  exit 1
fi

# 1. Terraform state storage. The account name is derived from the subscription, so a second run reuses it.
state_account=applytrack$(printf '%s' "$subscription" | md5sum | cut -c1-12)
az group create --name "$state_rg" --location "$location" --output none
az storage account create --name "$state_account" --resource-group "$state_rg" --location "$location" \
  --sku Standard_LRS --min-tls-version TLS1_2 --allow-blob-public-access false --output none
# Management-plane command: works with the Owner role alone (the data-plane variant needs a blob data role).
az storage container-rm create --storage-account "$state_account" --resource-group "$state_rg" --name tfstate --output none
state_account_id=$(az_value storage account show --name "$state_account" --resource-group "$state_rg" --query id)

# The signed-in owner also gets blob access to the state, so scripts/teardown.sh works from this machine.
me=$(az_value ad signed-in-user show --query id)
az role assignment create --assignee-object-id "$me" --assignee-principal-type User \
  --role "Storage Blob Data Contributor" --scope "$state_account_id" --output none

# 2. Identity for the deploy workflow, trusted only for this repository's runs on main.
az identity create --name "$identity" --resource-group "$state_rg" --location "$location" --output none
client_id=$(az_value identity show --name "$identity" --resource-group "$state_rg" --query clientId)
principal_id=$(az_value identity show --name "$identity" --resource-group "$state_rg" --query principalId)
az identity federated-credential create --identity-name "$identity" --resource-group "$state_rg" --name github-main \
  --issuer https://token.actions.githubusercontent.com --subject "repo:$repo:ref:refs/heads/main" \
  --audiences api://AzureADTokenExchange --output none
az role assignment create --assignee-object-id "$principal_id" --assignee-principal-type ServicePrincipal \
  --role Contributor --scope "/subscriptions/$subscription" --output none
az role assignment create --assignee-object-id "$principal_id" --assignee-principal-type ServicePrincipal \
  --role "Storage Blob Data Contributor" --scope "$state_account_id" --output none

cat <<OUT

Done. Role assignments can take a few minutes to become active.
Add these as GitHub repository secrets (Settings > Secrets and variables > Actions):
  AZURE_CLIENT_ID           $client_id
  AZURE_TENANT_ID           $tenant
  AZURE_SUBSCRIPTION_ID     $subscription
  TF_STATE_RESOURCE_GROUP   $state_rg
  TF_STATE_STORAGE_ACCOUNT  $state_account
  BUDGET_ALERT_EMAIL        <your e-mail address>
OUT
