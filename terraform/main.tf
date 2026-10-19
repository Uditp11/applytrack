# ApplyTrack on Azure: Container Apps (scales to zero) and PostgreSQL Flexible Server (B1ms), both inside the
# free offers at the time of writing. No container registry (the image comes from GitHub Container Registry)
# and no Log Analytics workspace, because both are billed.

resource "azurerm_resource_group" "main" {
  name     = "${var.name}-rg"
  location = var.location
}

resource "random_password" "db" {
  length  = 32
  special = false
}

resource "azurerm_postgresql_flexible_server" "db" {
  name                          = "${var.name}-db-${substr(md5(azurerm_resource_group.main.id), 0, 6)}"
  resource_group_name           = azurerm_resource_group.main.name
  location                      = azurerm_resource_group.main.location
  version                       = "17"
  sku_name                      = "B_Standard_B1ms"
  storage_mb                    = 32768
  backup_retention_days         = 7
  geo_redundant_backup_enabled  = false
  public_network_access_enabled = true
  administrator_login           = "applytrack"
  administrator_password        = random_password.db.result

  # Azure picks an availability zone on creation; do not try to move it on later applies.
  lifecycle {
    ignore_changes = [zone]
  }
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = "applytrack"
  server_id = azurerm_postgresql_flexible_server.db.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# 0.0.0.0 to 0.0.0.0 is Azure's notation for "allow connections from Azure services", which the Container App
# needs. TLS is required by the server and by the JDBC URL below.
resource "azurerm_postgresql_flexible_server_firewall_rule" "azure_services" {
  name             = "allow-azure-services"
  server_id        = azurerm_postgresql_flexible_server.db.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_container_app_environment" "main" {
  name                = "${var.name}-env"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
}

resource "azurerm_container_app" "app" {
  name                         = var.name
  resource_group_name          = azurerm_resource_group.main.name
  container_app_environment_id = azurerm_container_app_environment.main.id
  revision_mode                = "Single"

  secret {
    name  = "db-password"
    value = random_password.db.result
  }

  ingress {
    external_enabled = true
    target_port      = 8080

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    # Scale to zero when idle: no charges while nobody uses the demo.
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "applytrack"
      image  = var.image
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${azurerm_postgresql_flexible_server.db.fqdn}:5432/${azurerm_postgresql_flexible_server_database.app.name}?sslmode=require"
      }
      env {
        name  = "DB_USER"
        value = azurerm_postgresql_flexible_server.db.administrator_login
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      # The app is public here and nothing scrapes metrics, so only health stays exposed under /actuator.
      env {
        name  = "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE"
        value = "health"
      }

      startup_probe {
        transport               = "HTTP"
        path                    = "/actuator/health/liveness"
        port                    = 8080
        interval_seconds        = 5
        failure_count_threshold = 24
      }
      liveness_probe {
        transport = "HTTP"
        path      = "/actuator/health/liveness"
        port      = 8080
      }
      readiness_probe {
        transport = "HTTP"
        path      = "/actuator/health/readiness"
        port      = 8080
      }
    }
  }

  # The app runs Flyway against the database at startup, so the firewall must already let it in.
  depends_on = [azurerm_postgresql_flexible_server_firewall_rule.azure_services]
}

# Any actual spend above 1% of the budget sends an e-mail. Azure evaluates budgets with a delay of up to about a
# day, so this is a safety net, not an instant alarm.
resource "azurerm_consumption_budget_resource_group" "main" {
  name              = "${var.name}-budget"
  resource_group_id = azurerm_resource_group.main.id
  amount            = var.budget_amount
  time_grain        = "Monthly"

  time_period {
    start_date = var.budget_start_date
  }

  notification {
    enabled        = true
    operator       = "GreaterThan"
    threshold      = 1
    threshold_type = "Actual"
    contact_emails = [var.budget_alert_email]
  }

  # The deploy workflow passes the current month's first day; keep the start date of the first apply.
  lifecycle {
    ignore_changes = [time_period]
  }
}
