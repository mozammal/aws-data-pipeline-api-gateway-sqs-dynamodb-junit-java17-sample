package org.example.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.WeatherEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CreateWeatherEventFunction {

  private final String tableName = System.getenv("LOCATIONS_TABLE");
  private final String awsRegionEnv = System.getenv("AWS_REGION_NAME");
  private final Region region = Region.of(awsRegionEnv);

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
  ;

  private final DynamoDbClient dynamoDbClient;
  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<WeatherEvent> mappedTable;

  public CreateWeatherEventFunction() {
    this.dynamoDbClient = DynamoDbClient.builder().region(region).build();
    this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public CreateWeatherEventFunction(DynamoDbClient dynamoDbClient) {
    this.dynamoDbClient = dynamoDbClient;
    this.enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(this.dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public void apply(SQSEvent event) {
    for (var record : event.getRecords()) {
      try {
        putItem(record.getBody());
      } catch (Exception ex) {
        System.out.println("cant save the item to the DynamoDB table" + ex.getMessage());
        throw new RuntimeException(ex.getMessage());
      }
    }
  }

  WeatherEvent putItem(String item) throws JsonProcessingException {
    if (item.strip().length() > 1 && item.startsWith("'") && item.endsWith("'")) {
      item = item.substring(1, item.length() - 1);
    }

    var weatherEvent = objectMapper.readValue(item, WeatherEvent.class);
    mappedTable.putItem(r -> r.item(weatherEvent));
    return mappedTable.getItem(r -> r.key(k -> k.partitionValue(weatherEvent.getLocationName())));
  }
}
