variable "subscription_id" {
  description = "Azure subscription to deploy into."
  type        = string
}

variable "location" {
  description = "Azure region. Pick one where the free PostgreSQL B1ms offer is available."
  type        = string
  default     = "germanywestcentral"
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
}

variable "budget_amount" {
  description = "Monthly budget for the resource group, in the subscription's currency. Any spend above 1% of it alerts."
  type        = number
  default     = 1
}

variable "budget_start_date" {
  description = "First day of the month the budget starts, as YYYY-MM-01T00:00:00Z. Azure rejects dates in the past."
  type        = string
}
