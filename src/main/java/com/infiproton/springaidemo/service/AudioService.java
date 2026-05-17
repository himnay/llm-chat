package com.infiproton.springaidemo.service;

import lombok.AllArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AudioService {

    private static final Path AUDIO_DIR = Path.of(System.getProperty("java.io.tmpdir"), "spring-ai-audio");

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final OpenAiAudioSpeechModel speechModel;

    public byte[] textToSpeech(String text) {
        // 1. define options
        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .model("tts-1") // tts-1, tts-1-hd
                .voice("echo") // alloy, echo, fable, onyx, nova, shimmer
                .build();

        // 2. create prompt
        SpeechPrompt prompt = new SpeechPrompt(text, options);

        // 3. call LLM
        SpeechResponse response = speechModel.call(prompt);
        return response.getResult().getOutput();
    }

    public String speechToText(String storedFileName) {
        try {
            // 1. convert the stored file into Resource
            Path audioPath = AUDIO_DIR.resolve(storedFileName);
            byte[] audioBytes = Files.readAllBytes(audioPath);
            Resource audio = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return storedFileName;
                }
            };

            // 2. Prepare transcription options
            AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                    .model("whisper-1")
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                    .build();

            // 3. build the prompt
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audio, options);

            // 4. call transcription model
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);
            return response.getResult().getOutput();

        } catch (Exception e) {
            throw new RuntimeException("Failed to transcribe audio", e);
        }
    }

    public Map<String, Object> store(MultipartFile file) {
        try {
            Files.createDirectories(AUDIO_DIR);

            String fileId = UUID.randomUUID().toString();
            String storedFileName = fileId + "_" + file.getOriginalFilename();
            Path targetPath = AUDIO_DIR.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath);

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("originalFilename", file.getOriginalFilename());
            response.put("storedFileName", storedFileName);
            response.put("contentType", file.getContentType());
            response.put("size", file.getSize());
            return response;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store audio file", e);
        }
    }
}
