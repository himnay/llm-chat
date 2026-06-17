package com.org.llm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Faithfulness/groundedness check for RAG answers, mirroring
 * {@code llm-rag-pipeline}'s {@code GenerationEvaluator.isFaithful} — uses Spring AI's
 * {@link FactCheckingEvaluator} to verify every claim in the answer is entailed by the retrieved
 * context. Gated behind {@code app.rag.evaluate-faithfulness} (default {@code false}) since it
 * costs one extra LLM call per request; only worth running when there's actually RAG context to
 * check against.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerEvaluator {

    private final ChatClient.Builder ragChatClientBuilder;

    /**
     * Returns {@code true}/{@code false} for whether {@code answer} is grounded in
     * {@code context}, or {@code true} (fail-open) if the evaluator call itself errors.
     */
    public boolean isFaithful(String question, List<Document> context, String answer) {
        try {
            FactCheckingEvaluator evaluator = FactCheckingEvaluator.builder(ragChatClientBuilder).build();
            EvaluationRequest request = new EvaluationRequest(question, context, answer);
            EvaluationResponse response = evaluator.evaluate(request);
            boolean pass = response.isPass();
            log.debug("FactCheck: {} | question='{}'", pass ? "PASS" : "FAIL", question);
            return pass;
        } catch (Exception e) {
            log.warn("FactCheckingEvaluator failed ({}); defaulting to faithful=true", e.getMessage());
            return true;
        }
    }
}
