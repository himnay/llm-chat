package com.org.llm.tool;

import com.org.llm.model.ForecastResponse;
import com.org.llm.model.WeatherResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class WeatherTools {

    /** Bounded timeouts: a slow weather API must not hold the chat request (and its tool loop) forever. */
    private final RestClient restClient = RestClient.builder().requestFactory(boundedTimeouts()).build();

    @Value("${app.weather.api-key}")
    private String apiKey;

    @Tool(description = "Get weather forecast for a given city and date (yyyy-MM-dd). If date is not provided, defaults to today.")
    public WeatherResult getWeather(String city, String date) {
        log.info("Getting weather for city: {}", city);
        try {
            // HTTPS: the API key travels in the query string. Build a URI, not a String — handing
            // RestTemplate/RestClient an already-encoded string encodes it again ("New%2520York").
            UriComponentsBuilder uri = UriComponentsBuilder
                    .fromUriString("https://api.weatherapi.com/v1/forecast.json")
                    .queryParam("key", apiKey)
                    .queryParam("q", city);
            if (date != null && !date.isBlank()) {
                uri.queryParam("dt", date);   // omitted = today, as the tool description promises
            }

            ForecastResponse apiResponse = restClient.get()
                    .uri(uri.build().encode().toUri())
                    .retrieve()
                    .body(ForecastResponse.class);
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

    /** Bounded connect/read timeouts — the JDK client's default read timeout is infinite, so a hung upstream would pin the calling thread. */
    private static JdkClientHttpRequestFactory boundedTimeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }
}
