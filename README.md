## aws-api-gateway-dynamodb-sample

This project contains source code and supporting files for a serverless application
that you can deploy with Terraform. This time, we'll use AWS API Gateway, SQS, DynamoDB, adn Lambda to build a data
pipeline. When a POST request is made, one Lambda function will publish an event to SQS, while another Lambda function
consumes the event and persists it to DynamoDB. A third Lambda function is used to retrieve all events from DynamoDB
when a GET request is made.

It includes the following files and folders.

- /src/main/org/java/example - Code for the application's Lambda function.
- /src/test/java/org/example - Unit tests for the application code.
- /terraform - terraform IaC files for the application's AWS resources.

The application leverages multiple AWS resources, including Lambda functions, SQS, DynamoDB, and an API Gateway.

## Deploy the sample application

You may need the following tools.

* java17 - [Install the Java 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)
* Terraform - [Install Terraform](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli)

* To build and deploy your application for the first time, run the following in your shell:

```bash
export/set AWS_ACCESS_KEY_ID=ur_aws_access_key
export/set AWS_SECRET_ACCESS_KEY=ur_aws_secret
```

* create an S3 bucket so that you can upload your build artifacts into it. Bucket names are globally unique, so you may
  need to change the name

```bash
aws s3 mb s3://my-bucket-moz-61717
```

* Run the following commands to build the artifact

```bash
git clone https://github.com/mozammal/aws-data-pipeline-api-gateway-sqs-dynamodb-junit-java17-sample.git
cd aws-data-pipeline-api-gateway-sqs-dynamodb-junit-java17-sample
mvn clean package
```

* Navigate to the Terraform folder and deploy the application on AWS using the following commands

```bash
cd terraform
terraform init
terraform apply
terraform output
```

* You can now view the get_url and post_url in the console. Use the following JSON payload to make a POST request

```bash
{
    "locationName":"Mozammal, Malaga", 
    "temperature":91, 
    "timestamp":1564428897, 
    "latitude": 40.70, 
    "longitude": -73.99
}
```

## Cleanup

To delete the sample application that you created, you can run the following:

```bash
terraform  destroy
```
