package com.org.llm.client;

import com.org.llm.client.dto.ChatAgentChatRequest;
import com.org.llm.client.dto.ChatAgentChatResponse;
import com.org.llm.config.ChatAgentProperties;
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

    /** Chats. */
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

    private Duration timeout() {
        return Duration.ofSeconds(properties.getTimeoutSeconds());
    }
}
