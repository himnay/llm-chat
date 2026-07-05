package com.org.llm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ImageGenerateRequest(
        @NotBlank(message = "message is required")
        String message,
        @NotBlank(message = "style is required")
        String style,
        @NotNull(message = "count is required")
        @Positive(message = "count must be a positive number")
        Integer count
) {
}
