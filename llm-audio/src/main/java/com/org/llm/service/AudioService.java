package com.org.llm.service;

import com.openai.models.audio.AudioResponseFormat;
import com.org.llm.model.StoredAudio;
import lombok.AllArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AudioService {

    private static final Path AUDIO_DIR = Path.of(System.getProperty("java.io.tmpdir"), "spring-ai-audio");

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final OpenAiAudioSpeechModel speechModel;

    /** Returns the store. */
    public StoredAudio store(MultipartFile file) {
        try {
            Files.createDirectories(AUDIO_DIR);

            String fileId = UUID.randomUUID().toString();
            // keep only the last path segment of the client-supplied name — prevents path traversal
            String safeName = Path.of(Objects.requireNonNullElse(file.getOriginalFilename(), "audio"))
                    .getFileName().toString();
            String storedFileName = fileId + "_" + safeName;
            Path targetPath = AUDIO_DIR.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetPath);

            return new StoredAudio(fileId, file.getOriginalFilename(), storedFileName,
                    file.getContentType(), file.getSize());

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store audio file", e);
        }
    }

    /** Returns the speech to text. */
    public String speechToText(String storedFileName) {
        byte[] audioBytes;
        try {
            audioBytes = Files.readAllBytes(AUDIO_DIR.resolve(storedFileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read stored audio file: " + storedFileName, e);
        }
        Resource audio = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return storedFileName;
            }
        };

        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .responseFormat(AudioResponseFormat.JSON)
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audio, options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);
        return response.getResult().getOutput();
    }

    /** Returns the text to speech. */
    public byte[] textToSpeech(String text) {
        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .model("tts-1") // tts-1, tts-1-hd
                .voice("echo") // alloy, echo, fable, onyx, nova, shimmer
                .build();

        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);
        TextToSpeechResponse response = speechModel.call(prompt);
        return response.getResult().getOutput();
    }
}
