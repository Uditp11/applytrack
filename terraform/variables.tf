variable "subscription_id" {
  description = "Azure subscription to deploy into."
  type        = string
}

variable "location" {
  description = "Azure region. Azure for Students only allows some regions, and PostgreSQL B1ms is not offered everywhere; scripts/azure-bootstrap.sh checks the size."
  type        = string
  default     = "francecentral"
}

variable "name" {
  description = "Prefix for every resource name."
  type        = string
  default     = "applytrack"
}

variable "image" {
  description = "Container image to run, for example ghcr.io/uditp11/applytrack:<commit sha>."
  type        = string
}

variable "budget_alert_email" {
  description = "Where the budget alert e-mail goes."
  type        = string

  validation {
    condition     = can(regex("^[^@ ]+@[^@ ]+\\.[^@ ]+$", var.budget_alert_email))
    error_message = "The budget_alert_email value must be an e-mail address."
  }
}

variable "budget_amount" {
  description = "Monthly budget for the resource group, in the subscription's currency. Any spend above 1% of it alerts."
  type        = number
  default     = 1
}

variable "budget_start_date" {
  description = "First day of a month, as YYYY-MM-01T00:00:00Z. The deploy workflow passes the current month."
  type        = string
}
