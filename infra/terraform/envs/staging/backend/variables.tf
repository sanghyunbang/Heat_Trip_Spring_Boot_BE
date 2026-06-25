variable "aws_region" {
  description = "AWS region."
  type        = string
  default     = "ap-northeast-2"
}

variable "name_prefix" {
  description = "Resource name prefix."
  type        = string
  default     = "heat-trip-staging"
}

variable "vpc_id" {
  description = "Existing VPC ID."
  type        = string
}

variable "subnet_id" {
  description = "Existing subnet ID for the backend EC2."
  type        = string
}

variable "key_name" {
  description = "Existing EC2 key pair name."
  type        = string
}

variable "instance_type" {
  description = "Backend EC2 instance type."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size_gb" {
  description = "Backend root EBS volume size."
  type        = number
  default     = 40
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDRs allowed to SSH to backend EC2."
  type        = list(string)
  default     = []
}

variable "backend_app_allowed_cidr_blocks" {
  description = "CIDRs allowed to reach backend app port 8080. Prefer the loader private CIDR or VPC CIDR, not 0.0.0.0/0."
  type        = list(string)
  default     = []

  validation {
    condition     = length(var.backend_app_allowed_cidr_blocks) > 0
    error_message = "Set at least one backend_app_allowed_cidr_blocks entry, preferably the loader or VPC private CIDR."
  }
}

variable "repo_url" {
  description = "Public Git repository URL."
  type        = string

  validation {
    condition     = length(trimspace(var.repo_url)) > 0
    error_message = "repo_url must not be empty."
  }
}

variable "repo_branch" {
  description = "Git branch to deploy."
  type        = string
  default     = "main"
}

variable "app_env_ssm_parameter_name" {
  description = "SSM SecureString parameter name containing the backend .env content."
  type        = string
  default     = ""
}

variable "kms_key_arns" {
  description = "Optional KMS key ARNs for SecureString decrypt."
  type        = list(string)
  default     = []
}

variable "enable_observability" {
  description = "Run Prometheus/Grafana compose override."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Additional resource tags."
  type        = map(string)
  default     = {}
}
