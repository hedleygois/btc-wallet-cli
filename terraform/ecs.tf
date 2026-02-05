resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name    = "${var.project_name}-cluster"
    Project = var.project_name
  }
}

resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/fargate/${var.project_name}"
  retention_in_days = 7

  tags = {
    Name    = "${var.project_name}-ecs-log-group"
    Project = var.project_name
  }
}

resource "aws_ecs_task_definition" "main" {
  family                   = "${var.project_name}-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.container_cpu
  memory                   = var.container_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name        = var.project_name
      image       = var.container_image
      cpu         = var.container_cpu
      memory      = var.container_memory
      essential   = true
      portMappings = [
        {
          containerPort = var.app_port
          hostPort      = var.app_port
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = var.project_name
        }
      }
      # This is a CLI application, so the entrypoint needs to be considered.
      # When it becomes a web server, its entrypoint will likely be a command
      # to start the server. For now, the existing Dockerfile entrypoint is used.
      # If the future HTTP server has a different command to start, this needs
      # to be updated.
      # command = ["java", "-jar", "/app/lib/btc-wallet-app.jar"] # Example if it was a single jar
    }
  ])

  tags = {
    Name    = "${var.project_name}-task-def"
    Project = var.project_name
  }
}

resource "aws_ecs_service" "main" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.main.arn
  desired_count   = var.fargate_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_az1.id, aws_subnet.private_az2.id]
    security_groups = [aws_security_group.ecs_task.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = var.project_name
    container_port   = var.app_port
  }

  # Enable ECS Exec for debugging and interactive access if needed
  # requires:
  # - Session Manager Agent in the container (temurin-jre-alpine usually doesn't have it)
  # - "ssm:StartSession" permission in the task role
  # - "ecs:ExecuteCommand" permission for the user/role executing the command
  enable_execute_command = true

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy
  ]

  tags = {
    Name    = "${var.project_name}-ecs-service"
    Project = var.project_name
  }
}
