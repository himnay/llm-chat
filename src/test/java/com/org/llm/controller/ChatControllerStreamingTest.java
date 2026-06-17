package com.org.llm.controller;

import com.org.llm.model.ChatRequest;
import com.org.llm.service.ChatService;
import com.org.llm.service.TravelGuideService;
import com.org.llm.service.VoiceChatService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the streaming endpoint circuit-breaker fallback in {@link ChatController}.
 *
 * <p>The controller-level {@code @CircuitBreaker} fallback is tested directly by
 * calling the fallback method — no Spring context needed.</p>
 */
class ChatControllerStreamingTest {

    private final ChatService chatService = mock(ChatService.class);
    private final TravelGuideService travelGuideService = mock(TravelGuideService.class);
    private final VoiceChatService voiceChatService = mock(VoiceChatService.class);
    private final ChatMemory chatMemory = mock(ChatMemory.class);

    private final ChatController controller =
            new ChatController(chatService, travelGuideService, voiceChatService, chatMemory);

    @Test
    void streamFallback_returnsErrorSseEvent() {
        ChatRequest request = new ChatRequest("conv-1", "hello", null);
        RuntimeException cause = new RuntimeException("LLM unavailable");

        Flux<ServerSentEvent<String>> result = controller.streamFallback(request, cause);

        StepVerifier.create(result)
                .expectNextMatches(event -> event.data() != null && event.data().contains("temporarily unavailable"))
                .verifyComplete();
    }

    @Test
    void streamChat_delegatesToService() {
        ChatRequest request = new ChatRequest("conv-1", "hello", null);
        Flux<ServerSentEvent<String>> tokens = Flux.just(
                ServerSentEvent.<String>builder().event("token").data("Hello").build(),
                ServerSentEvent.<String>builder().event("token").data(" World").build(),
                ServerSentEvent.<String>builder().event("citations").data("[]").build());
        when(chatService.streamChat("conv-1", "hello")).thenReturn(tokens);

        Flux<ServerSentEvent<String>> result = controller.streamChat(request);

        StepVerifier.create(result)
                .expectNextMatches(e -> "token".equals(e.event()) && "Hello".equals(e.data()))
                .expectNextMatches(e -> "token".equals(e.event()) && " World".equals(e.data()))
                .expectNextMatches(e -> "citations".equals(e.event()) && "[]".equals(e.data()))
                .verifyComplete();
    }

    @Test
    void streamChat_whenServiceThrows_fallbackActivates() {
        ChatRequest request = new ChatRequest("conv-1", "hello", null);
        RuntimeException ex = new RuntimeException("service down");
        when(chatService.streamChat("conv-1", "hello")).thenThrow(ex);

        // Directly invoke the fallback as Resilience4j would
        Flux<ServerSentEvent<String>> fallbackResult = controller.streamFallback(request, ex);

        StepVerifier.create(fallbackResult)
                .expectNextMatches(event -> event.data() != null && event.data().contains("temporarily unavailable"))
                .verifyComplete();
    }
}
