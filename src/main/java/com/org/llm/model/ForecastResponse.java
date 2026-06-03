package com.org.llm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(Forecast forecast) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(List<ForecastDay> forecastday) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastDay(Day day) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Day(@JsonProperty("avgtemp_c") double avgtempC, Condition condition) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Condition(String text) {
    }
}