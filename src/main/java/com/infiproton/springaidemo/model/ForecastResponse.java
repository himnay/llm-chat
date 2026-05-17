package com.infiproton.springaidemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ForecastResponse {
    private Forecast forecast;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Forecast {
        private List<ForecastDay> forecastday;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ForecastDay {
        private Day day;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Day {
        private double avgtemp_c;
        private Condition condition;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Condition {
        private String text;
    }
}
