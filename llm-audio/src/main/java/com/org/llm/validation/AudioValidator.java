package com.org.llm.validation;

import com.org.llm.exception.AudioValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class AudioValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/webm",
            "audio/flac",
            "audio/x-m4a"
    );

    /** Validates. */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AudioValidationException("audio file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AudioValidationException(
                    "unsupported audio type: " + contentType + ". Allowed: " + ALLOWED_CONTENT_TYPES
            );
        }
    }
}
