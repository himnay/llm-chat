package com.org.llm.model;

import jakarta.validation.constraints.NotBlank;

public record TextToSqlRequest(
        @NotBlank(message = "question is required")
        String question,
        Integer maxRows,
        Boolean includeExplanation,
        Boolean dryRun
) {
}
