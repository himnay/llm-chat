package com.org.llm.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body from the gateway's {@code POST /llm/image} route
 * (subset of {@code llm-gateway}'s {@code ImageGenerationResponse}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayImageResponse(
        String model,
        List<String> images,
        Integer count,
        String error) {
}
