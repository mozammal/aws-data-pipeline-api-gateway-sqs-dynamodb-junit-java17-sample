terraform {
  required_version = ">= 1.0.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  object_source = "../${path.module}/${var.zip_path}"
}

resource "aws_s3_object" "file_upload" {
  bucket      = var.aws_bucket_name
  key         = var.zip_path
  source      = local.object_source
  source_hash = filemd5(local.object_source)
}

resource "aws_sqs_queue" "sqs_queue" {
  name = "sqs-queue"
}

resource "aws_iam_role" "weather_events_producer_role" {
  name               = "weather_events_producer_role"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action    = "sts:AssumeRole",
        Effect    = "Allow",
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role" "create_weather_event_function_role" {
  name               = "create_weather_event_function_role"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action    = "sts:AssumeRole",
        Effect    = "Allow",
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role" "get_all_weather_event_function_role" {
  name               = "get_all_weather_event_function_role"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action    = "sts:AssumeRole",
        Effect    = "Allow",
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "lambda_cloudwatch_policy" {
  name = "lambda_cloudwatch_policy"

  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "weather_event_producer_policy_attachment_cloudwatch" {
  role       = aws_iam_role.weather_events_producer_role.name
  policy_arn = aws_iam_policy.lambda_cloudwatch_policy.arn
}

resource "aws_iam_role_policy_attachment" "create_weather_event_function_policy_attachment_cloudwatch" {
  role       = aws_iam_role.create_weather_event_function_role.name
  policy_arn = aws_iam_policy.lambda_cloudwatch_policy.arn
}

resource "aws_iam_role_policy_attachment" "get_all_weather_event_function_policy_attachment_cloudwatch" {
  role       = aws_iam_role.get_all_weather_event_function_role.name
  policy_arn = aws_iam_policy.lambda_cloudwatch_policy.arn
}

resource "aws_iam_policy" "weather_event_producer_policy" {
  name = "weather_event_producer_policy"

  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action   = "sqs:SendMessage",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "sqs:GetQueueUrl",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "sqs:GetQueueAttributes",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "logs:*",
        Effect   = "Allow",
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

resource "aws_iam_policy" "create_weather_event_function_policy" {
  name = "create_weather_event_function_policy"

  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action   = "sqs:ReceiveMessage",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "sqs:DeleteMessage",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "sqs:GetQueueAttributes",
        Effect   = "Allow",
        Resource = aws_sqs_queue.sqs_queue.arn
      },
      {
        Action   = "logs:*",
        Effect   = "Allow",
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow",
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
        ],
        Resource = aws_dynamodb_table.locations_table.arn
      }
    ]
  })
}

resource "aws_iam_policy" "get_all_weather_event_function_policy" {
  name = "get_all_weather_event_function_policy"

  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Action   = "logs:*",
        Effect   = "Allow",
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow",
        Action = [
          "dynamodb:GetItem",
          "dynamodb:Scan",
        ],
        Resource = aws_dynamodb_table.locations_table.arn
      }
    ]
  })
}

resource "aws_dynamodb_table" "locations_table" {
  name         = "LocationsTable"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "locationName"
  attribute {
    name = "locationName"
    type = "S"
  }
}

resource "aws_iam_policy_attachment" "create_weather_event_function_policy_attachment" {
  policy_arn = aws_iam_policy.create_weather_event_function_policy.arn
  roles      = [aws_iam_role.create_weather_event_function_role.name]
  name       = "create_weather_event_function_policy_attachment"
}

resource "aws_iam_policy_attachment" "weather_event_producer_policy_attachment" {
  policy_arn = aws_iam_policy.weather_event_producer_policy.arn
  roles      = [aws_iam_role.weather_events_producer_role.name]
  name       = "weather_event_producer_policy_attachment"
}

resource "aws_iam_policy_attachment" "get_all_weather_event_function_policy_attachment" {
  policy_arn = aws_iam_policy.get_all_weather_event_function_policy.arn
  roles      = [aws_iam_role.get_all_weather_event_function_role.name]
  name       = "get_all_weather_event_function_policy_attachment"
}

resource "aws_lambda_function" "WeatherEventProducer" {
  function_name = "WeatherEventProducer"
  handler       = "org.example.handler.WeatherEventProducer::apply"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 20
  role          = aws_iam_role.weather_events_producer_role.arn
  s3_bucket     = aws_s3_object.file_upload.bucket
  s3_key        = aws_s3_object.file_upload.key
  environment {
    variables = {
      QUEUE_NAME      = aws_sqs_queue.sqs_queue.name
      AWS_REGION_NAME = var.aws_region
    }
  }
}

resource "aws_lambda_function" "CreateWeatherEventFunction" {
  function_name = "CreateWeatherEventFunction"
  handler       = "org.example.handler.CreateWeatherEventFunction::apply"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 20
  role          = aws_iam_role.create_weather_event_function_role.arn
  s3_bucket     = aws_s3_object.file_upload.bucket
  s3_key        = aws_s3_object.file_upload.key
  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
      AWS_REGION_NAME = var.aws_region
    }
  }
}

resource "aws_lambda_function" "GetAllWeatherEventFunction" {
  function_name = "GetAllWeatherEventFunction"
  handler       = "org.example.handler.GetAllWeatherEventFunction::apply"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 20
  role          = aws_iam_role.get_all_weather_event_function_role.arn
  s3_bucket     = aws_s3_object.file_upload.bucket
  s3_key        = aws_s3_object.file_upload.key
  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
      AWS_REGION_NAME = var.aws_region
    }
  }
}
resource "aws_lambda_event_source_mapping" "sqs_mapping" {
  event_source_arn = aws_sqs_queue.sqs_queue.arn
  function_name    = aws_lambda_function.CreateWeatherEventFunction.arn
  batch_size       = 1
}

resource "aws_api_gateway_rest_api" "api" {
  name = "aws-api-gateway-dynamodb-sample"
}

resource "aws_api_gateway_resource" "events_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = var.paths.events
}

resource "aws_api_gateway_method" "events_resource_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.events_resource.id
  http_method   = var.http_methods.POST
  authorization = var.authorization_none
}

resource "aws_lambda_permission" "WeatherEventProducer_permission" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.WeatherEventProducer.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/${var.stage_name}/${var.http_methods.POST}/${var.paths.events}"
}

resource "aws_api_gateway_integration" "weather_events_producer_resource_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.events_resource.id
  http_method             = aws_api_gateway_method.events_resource_method.http_method
  integration_http_method = var.integration_http_method
  type                    = var.integration_http_method_type
  uri                     = aws_lambda_function.WeatherEventProducer.invoke_arn
}

resource "aws_api_gateway_resource" "locations_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = var.paths.locations
}

resource "aws_api_gateway_method" "locations_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.locations_resource.id
  http_method   = var.http_methods.GET
  authorization = var.authorization_none
}

resource "aws_lambda_permission" "GetAllWeatherEventFunction_permission" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.GetAllWeatherEventFunction.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/${var.stage_name}/${var.http_methods.GET}/${var.paths.locations}"
}

resource "aws_api_gateway_integration" "locations_resource_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.locations_resource.id
  http_method             = aws_api_gateway_method.locations_method.http_method
  integration_http_method = var.integration_http_method
  type                    = var.integration_http_method_type
  uri                     = aws_lambda_function.GetAllWeatherEventFunction.invoke_arn
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = var.stage_name

  depends_on = [
    aws_api_gateway_integration.weather_events_producer_resource_integration,
    aws_api_gateway_integration.locations_resource_integration
  ]
}