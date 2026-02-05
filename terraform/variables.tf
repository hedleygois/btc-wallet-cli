variable "aws_region" {
  description = "The AWS region to deploy resources into."
  type        = string
  default     = "us-east-1" # Can be overridden in terraform.tfvars
}

variable "project_name" {
  description = "A unique name for the project, used to tag resources."
  type        = string
  default     = "btc-wallet-app"
}

variable "app_port" {
  description = "The port on which the application's HTTP interface will listen."
  type        = number
  default     = 8080
}

variable "ecr_repository_name" {
  description = "The name for the ECR repository."
  type        = string
  default     = "btc-wallet-app"
}

variable "container_image" {
  description = "The Docker image to deploy (e.g., <aws_account_id>.dkr.ecr.<aws_region>.amazonaws.com/<repository_name>:latest)."
  type        = string
  # This variable should be set in terraform.tfvars or via CLI,
  # or after the ECR repository has been created.
}

variable "container_cpu" {
  description = "The number of CPU units reserved for the container (e.g., 256 for 0.25 vCPU, 512 for 0.5 vCPU)."
  type        = number
  default     = 256
}

variable "container_memory" {
  description = "The amount of memory (in MiB) reserved for the container (e.g., 512, 1024, 2048)."
  type        = number
  default     = 512
}

variable "fargate_desired_count" {
  description = "The desired number of instantiations of the task definition to keep running on the cluster."
  type        = number
  default     = 1
}

variable "vpc_cidr_block" {
  description = "The CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_az1_cidr" {
  description = "The CIDR block for public subnet in AZ1."
  type        = string
  default     = "10.0.1.0/24"
}

variable "public_subnet_az2_cidr" {
  description = "The CIDR block for public subnet in AZ2."
  type        = string
  default     = "10.0.2.0/24"
}

variable "private_subnet_az1_cidr" {
  description = "The CIDR block for private subnet in AZ1."
  type        = string
  default     = "10.0.11.0/24"
}

variable "private_subnet_az2_cidr" {
  description = "The CIDR block for private subnet in AZ2."
  type        = string
  default     = "10.0.12.0/24"
}
