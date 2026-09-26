package com.org.llm.client;

import com.org.llm.client.dto.ChatAgentChatRequest;
import com.org.llm.client.dto.ChatAgentChatResponse;
import com.org.llm.config.ChatAgentProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Thin client over {@code llm-chat-agent}'s {@code POST /api/v1/chat}, used by
 * {@code VoiceChatService} to get the AI reply after transcribing voice input — voice chat used
 * to call {@code ChatService} in-process when both lived in the same app; now it's a separate
 * service reached over HTTP.
 */
@Slf4j
@Component
public class ChatAgentClient {

    private final WebClient webClient;
    private final ChatAgentProperties properties;

    public ChatAgentClient(WebClient chatAgentWebClient, ChatAgentProperties properties) {
        this.webClient = chatAgentWebClient;
        this.properties = properties;
    }

    /**
     * Asks {@code llm-chat-agent} for a reply. Retried, then short-circuited to a canned answer
     * while the agent is failing. These annotations used to sit on a {@code VoiceChatService}
     * method that the service called on itself, which bypassed the proxy — so they never ran.
     */
    @Retry(name = "llm-chat-agent")
    @CircuitBreaker(name = "llm-chat-agent", fallbackMethod = "chatFallback")
    public String chat(String conversationId, String message, String documentSource) {
        ChatAgentChatResponse response = webClient.post()
                .uri("/chat")
                .bodyValue(new ChatAgentChatRequest(conversationId, message, documentSource))
                .retrieve()
                .bodyToMono(ChatAgentChatResponse.class)
                .block(timeout());

        if (response == null || response.answer() == null) {
            throw new IllegalStateException("llm-chat-agent returned no answer");
        }
        return response.answer();
    }

    @SuppressWarnings("unused")
    private String chatFallback(String conversationId, String message, String documentSource, Throwable t) {
        log.warn("llm-chat-agent unavailable, answering with fallback: {}", t.getMessage());
        return "I'm temporarily unavailable. Please try again in a moment.";
    }

    private Duration timeout() {
        return Duration.ofSeconds(properties.getTimeoutSeconds());
    }
}
