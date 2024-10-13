package org.example.handler;

import org.example.entity.WeatherEvent;

final class TestHelper {
  private TestHelper() {
    throw new AssertionError();
  }

  static WeatherEvent getWeatherEvent() {
    WeatherEvent weatherEvent = new WeatherEvent();
    weatherEvent.setLocationName("Brooklyn");
    weatherEvent.setTemperature(91.0);
    weatherEvent.setTimestamp(1564428897L);
    weatherEvent.setLatitude(40.70);
    weatherEvent.setLongitude(-73.99);
    return weatherEvent;
  }
}
