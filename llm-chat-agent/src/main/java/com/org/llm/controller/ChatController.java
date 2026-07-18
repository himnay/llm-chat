package com.org.llm.controller;

import com.org.llm.model.ChatAnswer;
import com.org.llm.model.ChatRequest;
import com.org.llm.model.TravelPlan;
import com.org.llm.service.ChatService;
import com.org.llm.service.TravelGuideService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Conversational AI chat endpoints")
class ChatController {

    private final ChatService chatService;
    private final TravelGuideService travelGuideService;
    private final ChatMemory chatMemory;

    /** Chats. */
    @PostMapping
    @Operation(summary = "Send a chat message and receive a blocking response with RAG citations")
    public ChatAnswer chat(@Validated @RequestBody ChatRequest chatRequest) {
        return chatService.chat(chatRequest.conversationId(), chatRequest.message(), chatRequest.documentSource());
    }

    /** Returns the prepare travel plan. */
    @GetMapping("/travel-guide")
    @Operation(summary = "Generate a multi-day travel guide for a given city")
    public TravelPlan prepareTravelPlan(
            @NotBlank(message = "city is required") @RequestParam String city,
            @Positive(message = "days must be a positive number") @RequestParam Integer days) {
        return travelGuideService.prepareTravelPlan(city, days);
    }

    /** Fetches memory. */
    @GetMapping("/memory")
    @Operation(summary = "Retrieve paginated chat memory for a conversation")
    public Map<String, Object> fetchMemory(
            @NotBlank(message = "conversationId is required") @RequestParam String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Message> allMessages = chatMemory.get(conversationId);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Message> pagedMessages = toPage(allMessages, pageRequest);
        return Map.of(
                "content", pagedMessages.getContent(),
                "page", pagedMessages.getNumber(),
                "size", pagedMessages.getSize(),
                "totalElements", pagedMessages.getTotalElements()
        );
    }

    /** Streams chat. */
    @Operation(summary = "Stream a chat response as Server-Sent Events: 'token' events with answer "
            + "text, followed by one trailing 'citations' event with the RAG sources used (JSON array)")
    @CircuitBreaker(name = "llm-chat", fallbackMethod = "streamFallback")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Validated @RequestBody ChatRequest chatRequest) {
        return chatService.streamChat(chatRequest.conversationId(), chatRequest.message(), chatRequest.documentSource());
    }

    @SuppressWarnings("unused")
    Flux<ServerSentEvent<String>> streamFallback(ChatRequest chatRequest, Throwable t) {
        log.warn("ChatController stream circuit breaker fallback triggered: {}", t.getMessage());
        return Flux.just(ServerSentEvent.<String>builder()
                .event("token")
                .data("I'm temporarily unavailable. Please try again in a moment.")
                .build());
    }

    private static <T> Page<T> toPage(List<T> list, PageRequest pageRequest) {
        if (list == null) {
            return Page.empty(pageRequest);
        }
        int total = list.size();
        int fromIndex = (int) pageRequest.getOffset();
        if (fromIndex >= total) {
            return new PageImpl<>(List.of(), pageRequest, total);
        }
        int toIndex = Math.min(fromIndex + pageRequest.getPageSize(), total);
        return new PageImpl<>(list.subList(fromIndex, toIndex), pageRequest, total);
    }
}
