package com.org.llm.tool;

import com.org.llm.model.ForecastResponse;
import com.org.llm.model.WeatherResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class WeatherTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.weather.api-key}")
    private String apiKey;

    @Tool(description = "Get weather forecast for a given city and date (yyyy-MM-dd). If date is not provided, defaults to today.")
    public WeatherResult getWeather(String city, String date) {
        log.info("Getting weather for city: " + city);
        try {
            // Build API URL
            String url = UriComponentsBuilder
                    .fromUriString("http://api.weatherapi.com/v1/forecast.json")
                    .queryParam("key", apiKey)
                    .queryParam("q", city)
                    .queryParam("dt", date)
                    .toUriString();

            ForecastResponse apiResponse = restTemplate.getForObject(url, ForecastResponse.class);
            if (apiResponse == null) {
                return new WeatherResult(city, date, "N/A", "No data");
            }

            // Extract forecast
            ForecastResponse.ForecastDay forecastDay = apiResponse.forecast().forecastday().get(0);

            String condition = forecastDay.day().condition().text();
            double tempC = forecastDay.day().avgtempC();
            return new WeatherResult(city, date, tempC + " °C", condition);
        } catch (Exception e) {
            log.error("Error fetching weather for {} on {}: {}", city, date, e.getMessage(), e);
            return new WeatherResult(city, date, "N/A", "No data");
        }
    }
}
