#!/usr/bin/env bash
# Removes everything Terraform created for ApplyTrack on Azure, so nothing keeps running (or costing) after a demo.
# Needs: az login (or the deploy workflow's login), and the same backend settings the deploy used:
#   TF_STATE_RESOURCE_GROUP, TF_STATE_STORAGE_ACCOUNT, TF_STATE_CONTAINER (default: tfstate)
# Usage: scripts/teardown.sh
set -euo pipefail

: "${TF_STATE_RESOURCE_GROUP:?set TF_STATE_RESOURCE_GROUP}"
: "${TF_STATE_STORAGE_ACCOUNT:?set TF_STATE_STORAGE_ACCOUNT}"
: "${TF_VAR_subscription_id:?set TF_VAR_subscription_id}"

cd "$(dirname "$0")/../terraform"

terraform init -input=false \
  -backend-config="resource_group_name=$TF_STATE_RESOURCE_GROUP" \
  -backend-config="storage_account_name=$TF_STATE_STORAGE_ACCOUNT" \
  -backend-config="container_name=${TF_STATE_CONTAINER:-tfstate}" \
  -backend-config="key=applytrack.tfstate" \
  -backend-config="use_azuread_auth=true"

# Destroy needs every variable to have a value, but the values do not matter for deleting.
terraform destroy -auto-approve -input=false \
  -var="image=unused" \
  -var="budget_alert_email=unused@example.com" \
  -var="budget_start_date=2000-01-01T00:00:00Z"

echo "Everything Terraform created for ApplyTrack is deleted. Check the portal: the resource group should be gone."
