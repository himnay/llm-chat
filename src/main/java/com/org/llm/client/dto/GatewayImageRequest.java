package com.org.llm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for the gateway's {@code POST /llm/image} route
 * (mirrors {@code llm-gateway}'s {@code ImageGenerationRequest}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayImageRequest(
        String prompt,
        String model,
        String size,
        Integer n,
        @JsonProperty("response_format") String responseFormat) {
}
