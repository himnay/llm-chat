package com.org.llm.service;

import com.org.llm.model.StoredAudio;
import com.org.llm.validation.AudioValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Facade over the voice-chat pipeline (validate → store → transcribe → chat → synthesize),
 * so controllers stay free of multi-step orchestration.
 */
@Service
@RequiredArgsConstructor
public class VoiceChatService {

    private final AudioService audioService;
    private final ChatService chatService;
    private final AudioValidator audioValidator;

    /** Transcript of the uploaded audio plus the model's reply to it. */
    public record VoiceExchange(String transcript, String aiResponse) {
    }

    public VoiceExchange exchange(MultipartFile file) {
        audioValidator.validate(file);
        StoredAudio stored = audioService.store(file);
        String transcript = audioService.speechToText(stored.storedFileName());
        String aiResponse = chatService.chat(null, transcript).answer();
        return new VoiceExchange(transcript, aiResponse);
    }

    public byte[] speak(String text) {
        return audioService.textToSpeech(text);
    }
}
