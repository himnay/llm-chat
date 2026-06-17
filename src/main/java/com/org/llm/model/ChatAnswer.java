package com.org.llm.model;

import java.util.List;

/**
 * Result of a blocking chat turn: the generated answer plus RAG provenance.
 *
 * @param answer    the generated answer text
 * @param citations documents retrieved and used to ground the answer; empty when the turn didn't
 *                  go through RAG (e.g. {@code GatewayChatBackend}, which has no RAG integration)
 * @param faithful  {@code null} when the faithfulness check wasn't run (disabled via
 *                  {@code app.rag.evaluate-faithfulness}, or there were no citations to check
 *                  against); otherwise whether {@link com.org.llm.service.AnswerEvaluator} judged
 *                  the answer fully grounded in the retrieved context
 */
public record ChatAnswer(String answer, List<Citation> citations, Boolean faithful) {

    public static ChatAnswer withoutRag(String answer) {
        return new ChatAnswer(answer, List.of(), null);
    }
}
