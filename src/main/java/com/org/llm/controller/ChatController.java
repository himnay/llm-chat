package com.org.llm.controller;

import com.org.llm.model.ChatAnswer;
import com.org.llm.model.ChatRequest;
import com.org.llm.model.TravelPlan;
import com.org.llm.service.ChatService;
import com.org.llm.service.TravelGuideService;
import com.org.llm.service.VoiceChatService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Conversational AI chat endpoints")
class ChatController {

    private final ChatService chatService;
    private final TravelGuideService travelGuideService;
    private final VoiceChatService voiceChatService;
    private final ChatMemory chatMemory;

    @Operation(summary = "Send a chat message and receive a blocking response with RAG citations")
    @PostMapping
    public ChatAnswer chat(@Validated @RequestBody ChatRequest chatRequest) {
        return chatService.chat(chatRequest.conversationId(), chatRequest.message());
    }

    @Operation(summary = "Send an audio file, receive an AI audio response (voice chat)")
    @PostMapping("/audio/voice")
    public ResponseEntity<byte[]> voiceChat(@RequestParam("file") MultipartFile file) {
        VoiceChatService.VoiceExchange exchange = voiceChatService.exchange(file);
        byte[] audioResponse = voiceChatService.speak(exchange.aiResponse());

        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .header("X-Transcript", exchange.transcript())
                .header("X-AI-Response", exchange.aiResponse())
                .body(audioResponse);
    }

    @Operation(summary = "Send an audio file, receive transcript and AI text response")
    @PostMapping("/audio")
    public Map<String, Object> chatWithAudio(@RequestParam("file") MultipartFile file) {
        VoiceChatService.VoiceExchange exchange = voiceChatService.exchange(file);
        return Map.of("transcript", exchange.transcript(), "aiResponse", exchange.aiResponse());
    }

    @Operation(summary = "Generate a multi-day travel guide for a given city")
    @GetMapping("/travel-guide")
    public TravelPlan prepareTravelPlan(
            @NotBlank(message = "city is required") @RequestParam String city,
            @Positive(message = "days must be a positive number") @RequestParam Integer days) {
        return travelGuideService.prepareTravelPlan(city, days);
    }

    @Operation(summary = "Retrieve paginated chat memory for a conversation")
    @GetMapping("/memory")
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

    @Operation(summary = "Stream a chat response as Server-Sent Events: 'token' events with answer "
            + "text, followed by one trailing 'citations' event with the RAG sources used (JSON array)")
    @CircuitBreaker(name = "llm-chat", fallbackMethod = "streamFallback")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Validated @RequestBody ChatRequest chatRequest) {
        return chatService.streamChat(chatRequest.conversationId(), chatRequest.message());
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
