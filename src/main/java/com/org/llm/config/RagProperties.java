package com.org.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the local RAG pipeline (prefix {@code app.rag}).
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /**
     * Run {@link com.org.llm.service.AnswerEvaluator} (Spring AI's {@code FactCheckingEvaluator})
     * after each RAG-grounded answer to check it's entailed by the retrieved context. Off by
     * default — costs one extra LLM call per request.
     */
    private boolean evaluateFaithfulness = false;

    public boolean isEvaluateFaithfulness() { return evaluateFaithfulness; }
    public void setEvaluateFaithfulness(boolean evaluateFaithfulness) { this.evaluateFaithfulness = evaluateFaithfulness; }
}
