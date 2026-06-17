package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;

import java.util.List;

/**
 * Strategy interface: one implementation per {@link QueryTransformationTechnique}, each wrapping a
 * different Spring AI pre-retrieval query transformer/expander behind a uniform {@code List<String>}
 * result so callers don't need to care whether the underlying API returns one query or several.
 */
interface QueryTransformationStrategy {

    QueryTransformationTechnique technique();

    List<String> transform(QueryTransformRequest request);
}
