package com.org.llm.backend;

import com.org.llm.model.ChatAnswer;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * Strategy for where chat completions are executed: through {@code llm-gateway} or directly
 * against the local Spring AI {@code ChatClient}. Exactly one implementation is active per run,
 * selected at startup by {@code app.gateway.enabled} (see {@link GatewayChatBackend} /
 * {@link LocalChatBackend}).
 */
public interface ChatBackend {

    ChatAnswer chat(String systemPrompt, String conversationId, String message);

    /**
     * Streams the answer as SSE: zero or more {@code event: token} events carrying raw answer
     * text chunks, followed by exactly one trailing {@code event: citations} event carrying the
     * RAG citations (as JSON; an empty array when the backend doesn't do RAG, e.g. the gateway
     * path).
     */
    Flux<ServerSentEvent<String>> stream(String conversationId, String message);
}
