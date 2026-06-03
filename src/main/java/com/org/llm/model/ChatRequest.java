package com.org.llm.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        String conversationId,
        @NotBlank(message = "message is required")
        String message,
        String imageName
) {
}
