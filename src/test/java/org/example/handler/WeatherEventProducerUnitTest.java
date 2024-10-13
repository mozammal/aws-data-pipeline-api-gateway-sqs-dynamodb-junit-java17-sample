package org.example.handler;

import static org.example.handler.TestHelper.getWeatherEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class WeatherEventProducerUnitTest {
  @SystemStub
  private final EnvironmentVariables variables =
      new EnvironmentVariables("QUEUE_NAME", "fake_queue");

  @SystemStub
  private EnvironmentVariables awsRegion =
      new EnvironmentVariables("AWS_REGION_NAME", "eu-north-1");

  private DynamoDbEnhancedClient enhancedClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void apply_whenPOSTWeatherEvent_thenPublishToSQS_successfully()
      throws JsonProcessingException {
    WeatherEvent weatherEvent = getWeatherEvent();
    String json = objectMapper.writeValueAsString(weatherEvent);
    ApiGatewayRequestEvent apiGatewayRequestEvent = new ApiGatewayRequestEvent(json);

    AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
    var eventProducer = new WeatherEventProducer(sqs);
    GetQueueUrlResult mockResult = new GetQueueUrlResult().withQueueUrl("fake-queue");
    when(sqs.getQueueUrl(anyString())).thenReturn(mockResult);

    ApiGatewayResponseEvent apiGatewayResponseEvent = eventProducer.apply(apiGatewayRequestEvent);

    assertEquals(json, apiGatewayResponseEvent.body());
    assertEquals(200, apiGatewayResponseEvent.statusCode());
    ArgumentCaptor<SendMessageRequest> sendMessageRequestArgumentCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    Mockito.verify(sqs, Mockito.times(1)).sendMessage(sendMessageRequestArgumentCaptor.capture());
    List<SendMessageRequest> sendMessageRequests = sendMessageRequestArgumentCaptor.getAllValues();
    List<String> messageBody =
        sendMessageRequests.stream().map(SendMessageRequest::getMessageBody).toList();
    assertIterableEquals(
        List.of(
            "{\"locationName\":\"Brooklyn\",\"temperature\":91.0,\"timestamp\":1564428897,\"longitude\":-73.99,\"latitude\":40.7}"),
        messageBody);
  }
}
