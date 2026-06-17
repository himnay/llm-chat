package com.org.llm.service;

import com.org.llm.backend.ChatBackend;
import com.org.llm.model.ChatAnswer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatBackend chatBackend;

    @CircuitBreaker(name = "llm-chat", fallbackMethod = "chatFallback")
    @Retry(name = "llm-chat")
    public ChatAnswer chat(String conversationId, String message) {
        return chatBackend.chat(systemPrompt(), normalizeConversationId(conversationId), message);
    }

    public Flux<ServerSentEvent<String>> streamChat(String conversationId, String message) {
        return chatBackend.stream(normalizeConversationId(conversationId), message);
    }

    @SuppressWarnings("unused")
    private ChatAnswer chatFallback(String conversationId, String message, Throwable t) {
        log.warn("ChatService circuit breaker fallback for conversationId={}: {}", conversationId, t.getMessage());
        return new ChatAnswer("I'm temporarily unavailable. Please try again in a moment.", List.of(), null);
    }

    private static String systemPrompt() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return "Today's date is " + today + ". " +
                "You are a friendly travel guide. Suggest 3 attractions and 1 food item.";
    }

    private static String normalizeConversationId(String conversationId) {
        return (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
    }
}
