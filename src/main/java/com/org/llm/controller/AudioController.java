package com.org.llm.controller;

import com.org.llm.service.AudioService;
import com.org.llm.validation.AudioValidator;
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
@RequestMapping("/audio")
class AudioController {

    private final AudioService audioService;
    private final AudioValidator audioValidator;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAudio(@RequestParam("file") MultipartFile file) {
        audioValidator.validate(file);
        return ResponseEntity.ok(audioService.store(file));
    }

    @PostMapping("/to-speech")
    public ResponseEntity<byte[]> textToSpeech(
            @NotBlank(message = "text is required") @RequestParam("text") String text) {
        byte[] audio = audioService.textToSpeech(text);
        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .body(audio);
    }

    @PostMapping("/to-text")
    public ResponseEntity<Map<String, Object>> speechToText(@RequestParam("file") MultipartFile file) {
        audioValidator.validate(file);
        Map<String, Object> uploadResult = audioService.store(file);
        String storedFileName = (String) uploadResult.get("storedFileName");
        return ResponseEntity.ok(Map.of("text", audioService.speechToText(storedFileName)));
    }
}
