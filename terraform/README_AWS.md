# AWS ECS Deployment Instructions for BTC Wallet Management App

This document provides instructions for deploying the BTC Wallet Management App to AWS Elastic Container Service (ECS) using the provided Terraform configuration.

## Prerequisites

1.  **AWS Account**: You need an active AWS account.
2.  **AWS CLI**: Configured with credentials that have permissions to create and manage EC2, ECS, ECR, IAM, and CloudWatch resources.
3.  **Terraform**: Installed and configured.
4.  **Docker**: Installed and running.
5.  **Java 21 and Gradle**: For building the application.

## Deployment Steps

### 1. Build and Push Docker Image

First, you need to build the Docker image of your application and push it to Amazon Elastic Container Registry (ECR).

Navigate to the root directory of your project where the `Dockerfile` is located.

```bash
cd /Users/hedleyluna/dev/blockchain-java
```

**a. Authenticate Docker to ECR:**

Replace `YOUR_AWS_ACCOUNT_ID` and `YOUR_AWS_REGION` with your actual AWS account ID and desired region (e.g., `us-east-1`).

```bash
aws ecr get-login-password --region YOUR_AWS_REGION | docker login --username AWS --password-stdin YOUR_AWS_ACCOUNT_ID.dkr.ecr.YOUR_AWS_REGION.amazonaws.com
```

**b. Build the Docker Image:**

This command builds the Docker image. Make sure to replace `YOUR_AWS_ACCOUNT_ID` and `YOUR_AWS_REGION` with your details. The `btc-wallet-app` is the default repository name defined in `terraform/variables.tf`.

```bash
docker build -t YOUR_AWS_ACCOUNT_ID.dkr.ecr.YOUR_AWS_REGION.amazonaws.com/btc-wallet-app:latest .
```

**c. Push the Docker Image to ECR:**

```bash
docker push YOUR_AWS_ACCOUNT_ID.dkr.ecr.YOUR_AWS_REGION.amazonaws.com/btc-wallet-app:latest
```

### 2. Configure Terraform

Navigate to the `terraform/` directory.

```bash
cd terraform/
```

**a. Create `terraform.tfvars` file:**

Copy the example variables file and fill in your specific values.

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```terraform
# terraform.tfvars
aws_region        = "YOUR_AWS_REGION" # e.g., "us-east-1"
container_image   = "YOUR_AWS_ACCOUNT_ID.dkr.ecr.YOUR_AWS_REGION.amazonaws.com/btc-wallet-app:latest"
# Other variables can be left at their default or overridden if needed.
# For instance, if you decide to change the application port:
# app_port = 80
```

**b. Initialize Terraform:**

```bash
terraform init
```

### 3. Deploy Infrastructure with Terraform

**a. Review the plan:**

```bash
terraform plan
```

Review the proposed changes to ensure they match your expectations.

**b. Apply the changes:**

```bash
terraform apply
```

Type `yes` when prompted to confirm the deployment.

### 4. Verify Deployment

After Terraform applies successfully, you can get the ALB DNS name from the outputs:

```bash
terraform output alb_dns_name
```

Once your application's HTTP interface is implemented and deployed, you can access it via this DNS name. It might take a few minutes for the ECS service to start and the ALB health checks to pass.

You can monitor the ECS service and task status in the AWS ECS console. Logs will be available in AWS CloudWatch under the log group `/ecs/fargate/btc-wallet-app`.

### 5. Cleaning Up (Optional)

To destroy all the AWS resources created by Terraform, run:

```bash
terraform destroy
```

Type `yes` when prompted to confirm the destruction.

**Important Note on ECS Service & CLI application:**
The current application is a Command-Line Interface (CLI) tool. The Terraform configuration assumes that you will integrate an HTTP interface that listens on port `8080` (or `app_port` you define). Until that HTTP interface is implemented, the ECS service health checks on the ALB will likely fail, and the service might not stay running as expected.

If you wish to run the CLI application as a one-off task or to interact with it directly, you might consider using `aws ecs run-task` and `aws ecs execute-command` after the basic infrastructure (VPC, ECR, ECS Cluster, IAM roles) is set up. The provided Terraform configuration includes `enable_execute_command = true` on the ECS service for future debugging and interactive access if needed.
