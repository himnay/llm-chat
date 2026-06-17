package com.org.llm.rag;

/**
 * Discriminator key for the Strategy pattern in {@link QueryTransformationService}: each value
 * maps to exactly one {@link QueryTransformationStrategy} bean wrapping a Spring AI RAG transformer.
 */
public enum QueryTransformationTechnique {
    REWRITE,
    TRANSLATE,
    COMPRESS,
    MULTI_QUERY_EXPAND
}
