package org.example.handler;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;

public final class WeatherEventProducer {
  private static final String QUEUE_NAME = "QUEUE_NAME";
  private final AmazonSQS amazonSQS;
  private final String queueName;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WeatherEventProducer() {
    this(AmazonSQSClientBuilder.standard().withRegion(System.getenv("AWS_REGION_NAME")).build());
  }

  public WeatherEventProducer(AmazonSQS amazonSQS) {
    this.amazonSQS = amazonSQS;
    this.queueName = System.getenv(QUEUE_NAME);

    if (this.queueName == null) {
      throw new RuntimeException(String.format("%s is missing", QUEUE_NAME));
    }
  }

  public ApiGatewayResponseEvent apply(ApiGatewayRequestEvent request) {
    try {
      System.out.println(request);
      objectMapper.readValue(request.body(), WeatherEvent.class);
      publishToSQS(request.body());
      return new ApiGatewayResponseEvent(200, request.body());
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }

  private void publishToSQS(String message) {
    System.out.println("Sending event to SQS:");
    SendMessageRequest sendMessageRequest = getSendMessageRequest(message);
    amazonSQS.sendMessage(sendMessageRequest);
  }

  private SendMessageRequest getSendMessageRequest(String message) {
    return new SendMessageRequest()
        .withMessageBody(message)
        .withQueueUrl(amazonSQS.getQueueUrl(queueName).getQueueUrl())
        .withDelaySeconds(3);
  }
}
