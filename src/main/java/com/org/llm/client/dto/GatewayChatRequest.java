package com.org.llm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for the gateway's {@code POST /llm/chat} and {@code /llm/query} routes.
 * Mirrors {@code llm-gateway}'s {@code LlmRequest} (snake_case wire format).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayChatRequest(
        String prompt,
        String provider,
        String model,
        @JsonProperty("system_prompt") String systemPrompt,
        @JsonProperty("session_id") String sessionId) {
}
