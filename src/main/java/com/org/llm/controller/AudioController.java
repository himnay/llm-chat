package com.org.llm.controller;

import com.org.llm.model.StoredAudio;
import com.org.llm.service.AudioService;
import com.org.llm.validation.AudioValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audio")
@Tag(name = "Audio", description = "Audio upload, transcription and text-to-speech endpoints")
class AudioController {

    private final AudioService audioService;
    private final AudioValidator audioValidator;

    @Operation(summary = "Upload an audio file for storage")
    @PostMapping("/upload")
    public ResponseEntity<StoredAudio> uploadAudio(@RequestParam("file") MultipartFile file) {
        audioValidator.validate(file);
        return ResponseEntity.ok(audioService.store(file));
    }

    @Operation(summary = "Convert text to speech and return MP3 audio bytes")
    @PostMapping("/to-speech")
    public ResponseEntity<byte[]> textToSpeech(
            @NotBlank(message = "text is required") @RequestParam("text") String text) {
        byte[] audio = audioService.textToSpeech(text);
        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .body(audio);
    }

    @Operation(summary = "Transcribe an audio file to text using Whisper")
    @PostMapping("/to-text")
    public ResponseEntity<Map<String, Object>> speechToText(@RequestParam("file") MultipartFile file) {
        audioValidator.validate(file);
        StoredAudio stored = audioService.store(file);
        return ResponseEntity.ok(Map.of("text", audioService.speechToText(stored.storedFileName())));
    }
}
