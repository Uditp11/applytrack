output "app_url" {
  description = "Public URL of the app."
  value       = "https://${azurerm_container_app.app.ingress[0].fqdn}"
}

output "resource_group" {
  description = "Everything lives in this resource group."
  value       = azurerm_resource_group.main.name
}
