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
  description = "Existing subnet ID for the loader EC2."
  type        = string
}

variable "key_name" {
  description = "Existing EC2 key pair name."
  type        = string
}

variable "instance_type" {
  description = "Loader EC2 instance type."
  type        = string
  default     = "t3.small"
}

variable "root_volume_size_gb" {
  description = "Loader root EBS volume size."
  type        = number
  default     = 30
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDRs allowed to SSH to loader EC2."
  type        = list(string)
  default     = []
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
  description = "Git branch to clone."
  type        = string
  default     = "main"
}

variable "backend_base_url" {
  description = "Backend URL used by k6, usually http://<backend-private-ip>:8080."
  type        = string

  validation {
    condition     = can(regex("^https?://", var.backend_base_url))
    error_message = "backend_base_url must start with http:// or https://."
  }
}

variable "k6_env_ssm_parameter_name" {
  description = "Optional SSM SecureString parameter containing k6 env values."
  type        = string
  default     = ""
}

variable "kms_key_arns" {
  description = "Optional KMS key ARNs for SecureString decrypt."
  type        = list(string)
  default     = []
}

variable "default_vus" {
  description = "Default VUS written to k6.env when no SSM k6 env is provided."
  type        = number
  default     = 20
}

variable "default_duration" {
  description = "Default duration written to k6.env when no SSM k6 env is provided."
  type        = string
  default     = "10m"
}

variable "tags" {
  description = "Additional resource tags."
  type        = map(string)
  default     = {}
}
