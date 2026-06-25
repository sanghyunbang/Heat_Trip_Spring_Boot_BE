variable "name" {
  description = "Name prefix for EC2, security group, and IAM resources."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the instance will run."
  type        = string
}

variable "subnet_id" {
  description = "Subnet ID where the instance will run."
  type        = string
}

variable "ami_id" {
  description = "Optional AMI ID. Defaults to latest Amazon Linux 2023 x86_64."
  type        = string
  default     = ""
}

variable "instance_type" {
  description = "EC2 instance type."
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "Existing EC2 key pair name."
  type        = string
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDR blocks allowed to SSH to the host."
  type        = list(string)
  default     = []
}

variable "ingress_rules" {
  description = "Additional security group ingress rules."
  type = list(object({
    description = string
    from_port   = number
    to_port     = number
    protocol    = string
    cidr_blocks = list(string)
  }))
  default = []
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size."
  type        = number
  default     = 30
}

variable "startup_commands" {
  description = "Shell commands appended after base Docker/Git setup."
  type        = string
  default     = ""
}

variable "iam_ssm_parameter_arns" {
  description = "SSM parameter ARNs this instance may read."
  type        = list(string)
  default     = []
}

variable "kms_key_arns" {
  description = "Optional KMS key ARNs for decrypting SecureString parameters."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags applied to all resources."
  type        = map(string)
  default     = {}
}

