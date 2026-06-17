package com.org.llm.model;

import com.org.llm.rag.QueryTransformationTechnique;

import java.util.List;

public record QueryTransformResponse(
        QueryTransformationTechnique technique,
        String originalQuery,
        List<String> transformedQueries
) {
}
