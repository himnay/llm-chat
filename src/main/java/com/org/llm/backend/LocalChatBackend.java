package com.org.llm.backend;

import com.org.llm.config.RagProperties;
import com.org.llm.model.ChatAnswer;
import com.org.llm.model.Citation;
import com.org.llm.service.AnswerEvaluator;
import com.org.llm.tool.ContactsTool;
import com.org.llm.tool.WeatherTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Calls OpenAI directly via the local Spring AI {@code ChatClient}, with JDBC-backed
 * conversation memory, the weather/contacts tools, and RAG over the corporate travel-policy /
 * events documents (via {@link RetrievalAugmentationAdvisor}, see {@code RagConfig}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.gateway", name = "enabled", havingValue = "false")
public class LocalChatBackend implements ChatBackend {

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final WeatherTools weatherTools;
    private final ContactsTool contactsTool;
    private final AnswerEvaluator answerEvaluator;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    @Override
    public ChatAnswer chat(String systemPrompt, String conversationId, String message) {
        // RetrievalAugmentationAdvisor runs after the default MessageChatMemoryAdvisor, so its
        // CompressionQueryTransformer sees the history already prepended to the prompt and can fold
        // it with this message into a standalone query before hitting the vector store.
        ChatClientResponse response = chatClient.prompt()
                .system(systemPrompt)
                .advisors(spec -> spec
                        .advisors(retrievalAugmentationAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(weatherTools, contactsTool)
                .user(message)
                .call()
                .chatClientResponse();

        String answer = response.chatResponse() != null ? response.chatResponse().getResult().getOutput().getText() : null;
        List<Document> documents = retrievedDocuments(response);
        List<Citation> citations = documents.stream().map(Citation::from).toList();
        Boolean faithful = evaluateFaithfulness(message, documents, answer, citations);
        return new ChatAnswer(answer, citations, faithful);
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(String conversationId, String message) {
        // RetrievalAugmentationAdvisor's `before()` runs once, up front, and stashes the
        // retrieved documents in the advisor context under DOCUMENT_CONTEXT — that context map
        // is then carried on every ChatClientResponse emitted for this call, so each chunk's
        // citations are identical. We capture them from the last chunk seen (cheap — no second
        // retrieval call) and emit them as one trailing "citations" SSE event after the token
        // stream completes, so the token-by-token UX is unaffected.
        AtomicReference<List<Citation>> lastCitations = new AtomicReference<>(List.of());
        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
                .advisors(spec -> spec
                        .advisors(retrievalAugmentationAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(weatherTools, contactsTool)
                .user(message)
                .stream()
                .chatClientResponse()
                .doOnNext(response -> lastCitations.set(extractCitations(response)))
                .map(this::tokenEvent);

        return tokens
                .concatWith(Flux.defer(() -> Flux.just(citationsEvent(lastCitations.get()))))
                .onErrorResume(throwable -> {
                    log.error("Error occurred in the stream", throwable);
                    return Flux.error(new IllegalStateException(
                            "Error occurred in the stream: %s".formatted(throwable.getMessage())));
                });
    }

    /**
     * Reads the documents {@link RetrievalAugmentationAdvisor} retrieved for this call back out
     * of the advisor context, keyed under the verified {@code DOCUMENT_CONTEXT} constant (Spring
     * AI 2.0.0 {@code org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor}).
     */
    private List<Document> retrievedDocuments(ChatClientResponse response) {
        Object documentContext = response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(documentContext instanceof List<?> documents)) {
            return List.of();
        }
        return documents.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .toList();
    }

    private List<Citation> extractCitations(ChatClientResponse response) {
        return retrievedDocuments(response).stream().map(Citation::from).toList();
    }

    private Boolean evaluateFaithfulness(String question, List<Document> documents, String answer,
                                          List<Citation> citations) {
        if (!ragProperties.isEvaluateFaithfulness() || citations.isEmpty() || answer == null) {
            return null;
        }
        return answerEvaluator.isFaithful(question, documents, answer);
    }

    private ServerSentEvent<String> tokenEvent(ChatClientResponse response) {
        String token = response.chatResponse() != null ? response.chatResponse().getResult().getOutput().getText() : null;
        return ServerSentEvent.<String>builder()
                .event("token")
                .data(token != null ? token : "")
                .build();
    }

    private ServerSentEvent<String> citationsEvent(List<Citation> citations) {
        String json = writeJson(citations);
        return ServerSentEvent.<String>builder()
                .event("citations")
                .data(json)
                .build();
    }

    private String writeJson(List<Citation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            log.warn("Failed to serialize citations: {}", e.getMessage());
            return "[]";
        }
    }
}
