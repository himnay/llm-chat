package com.org.llm.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response body from the gateway's chat/query routes (subset of {@code llm-gateway}'s
 * {@code LlmResponse}). The gateway populates {@code content} (and mirrors it in {@code response}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayChatResponse(
        String content,
        String response,
        String provider,
        String model,
        String error) {

    /** The assistant text, tolerating either the {@code content} or {@code response} field. */
    public String text() {
        return content != null ? content : response;
    }
}
