package com.org.llm.controller;

import com.org.llm.service.VoiceChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Voice-chat endpoints: transcribe the uploaded audio, get the AI reply from
 * {@code llm-chat-agent} (over HTTP via {@code ChatAgentClient}), and optionally synthesize the
 * reply back to audio. Previously lived on {@code llm-chat}'s {@code ChatController} before the
 * chat/audio split.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Tag(name = "Voice Chat", description = "Voice-to-voice and voice-to-text chat endpoints")
class VoiceChatController {

    private final VoiceChatService voiceChatService;

    /** Returns the voice chat. */
    @PostMapping("/audio/voice")
    @Operation(summary = "Send an audio file, receive an AI audio response (voice chat)")
    public ResponseEntity<byte[]> voiceChat(@RequestParam("file") MultipartFile file) {
        VoiceChatService.VoiceExchange exchange = voiceChatService.exchange(file);
        byte[] audioResponse = voiceChatService.speak(exchange.aiResponse());

        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .header("X-Transcript", exchange.transcript())
                .header("X-AI-Response", exchange.aiResponse())
                .body(audioResponse);
    }

    /** Chats with audio. */
    @PostMapping("/audio")
    @Operation(summary = "Send an audio file, receive transcript and AI text response")
    public Map<String, Object> chatWithAudio(@RequestParam("file") MultipartFile file) {
        VoiceChatService.VoiceExchange exchange = voiceChatService.exchange(file);
        return Map.of("transcript", exchange.transcript(), "aiResponse", exchange.aiResponse());
    }
}
