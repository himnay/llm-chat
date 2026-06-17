package com.org.llm.model;

import com.org.llm.rag.QueryTransformationTechnique;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @param history alternating prior turns, oldest first, starting with the user
 *                 (e.g. {@code ["What is the capital of Denmark?", "Copenhagen."]}) — only
 *                 read by {@link QueryTransformationTechnique#COMPRESS}
 * @param targetLanguage only read by {@link QueryTransformationTechnique#TRANSLATE}; defaults to English
 */
public record QueryTransformRequest(
        @NotNull(message = "technique is required")
        QueryTransformationTechnique technique,
        @NotBlank(message = "query is required")
        String query,
        List<String> history,
        String targetLanguage
) {
}
