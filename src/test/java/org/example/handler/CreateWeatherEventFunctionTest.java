package org.example.handler;

import static org.example.handler.TestHelper.getWeatherEvent;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class CreateWeatherEventFunctionTest {
  @SystemStub
  private EnvironmentVariables tableName =
      new EnvironmentVariables("LOCATIONS_TABLE", "fake_table");

  @SystemStub
  private EnvironmentVariables awsRegion =
      new EnvironmentVariables("AWS_REGION_NAME", "eu-north-1");

  private DynamoDbEnhancedClient enhancedClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private DynamoDBProxyServer server;
  private DynamoDbTable<WeatherEvent> mappedTable;

  private DynamoDbClient dynamoDbClient;

  @BeforeEach
  void setup() throws Exception {
    server =
        ServerRunner.createServerFromCommandLineArgs(new String[] {"-inMemory", "-port", "8080"});
    server.start();
    dynamoDbClient = createClient();
    enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    mappedTable =
        enhancedClient.table(
            tableName.getVariables().get("LOCATIONS_TABLE"),
            TableSchema.fromBean(WeatherEvent.class));
    mappedTable.createTable();
  }

  @AfterEach
  public void deleteTable() throws Exception {
    server.stop();
  }

  private DynamoDbClient createClient() {
    String endpoint = String.format("http://localhost:%d", 8080);
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.EU_NORTH_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("dummykey", "dummysecret")))
        .build();
  }

  @Test
  void putItem_saveItems_Returns_Successfully() throws IOException {
    var createWeatherEventFunction = new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent expectedWeatherEvent = getWeatherEvent();
    String json = objectMapper.writeValueAsString(getWeatherEvent());

    WeatherEvent actualWeatherEvent = createWeatherEventFunction.putItem(json);

    Assertions.assertEquals(actualWeatherEvent, expectedWeatherEvent);
  }

  @Test
  void apply_ValidApiGatewayRequestEvent_returns_successfully() throws IOException {
    var weatherEventLambda = new CreateWeatherEventFunction(dynamoDbClient);
    var getAllWeatherEventFunction = new GetAllWeatherEventFunction(dynamoDbClient);
    WeatherEvent weatherEvent = getWeatherEvent();
    String weatherEventWithEscapeQuote =
        "'" + objectMapper.writeValueAsString(weatherEvent).replace("\"", "\\\"") + "'";
    String sqsRecord =
        "{ \"records\": [ { \"messageId\": \"1\", \"body\": "
            + "\""
            + weatherEventWithEscapeQuote
            + "\" } ] }";
    SQSEvent sqsEvent = objectMapper.readValue(sqsRecord, SQSEvent.class);

    weatherEventLambda.apply(sqsEvent);
    ApiGatewayResponseEvent apiGatewayResponseEvent =
        getAllWeatherEventFunction.apply(new ApiGatewayRequestEvent(""));
    List<WeatherEvent> weatherEvents =
        objectMapper.readValue(apiGatewayResponseEvent.body(), new TypeReference<>() {});

    Assertions.assertEquals(1, weatherEvents.size());
    Assertions.assertIterableEquals(Arrays.asList(weatherEvent), weatherEvents);
  }

  @Test
  void apply_InValidApiGatewayRequestEvent_throws_exception() throws IOException {
    var weatherEventLambda = new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent weatherEvent = getWeatherEvent();
    String invalidWeatherEvent =
        objectMapper.writeValueAsString(weatherEvent).replace("locationName", "locationName123");
    String weatherEventWithEscapeQuote = "'" + invalidWeatherEvent.replace("\"", "\\\"") + "'";
    String sqsRecord =
        "{ \"records\": [ { \"messageId\": \"1\", \"body\": "
            + "\""
            + weatherEventWithEscapeQuote
            + "\" } ] }";
    SQSEvent sqsEvent = objectMapper.readValue(sqsRecord, SQSEvent.class);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          weatherEventLambda.apply(sqsEvent);
        });
  }
}
