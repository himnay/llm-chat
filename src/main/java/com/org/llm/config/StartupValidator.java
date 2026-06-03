package com.org.llm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fail-loud startup checks. Warns when required external credentials are unset or still a
 * placeholder, so misconfiguration surfaces at boot rather than on the first request.
 */
@Slf4j
@Component
public class StartupValidator implements ApplicationRunner {

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.stabilityai.api-key:}")
    private String stabilityApiKey;

    @Value("${app.security.auth-enabled:true}")
    private boolean authEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (isMissing(openAiApiKey)) {
            log.warn("CONFIG | OPENAI_API_KEY is unset or a placeholder — chat/audio/embedding calls will fail at runtime.");
        }
        if (isMissing(stabilityApiKey)) {
            log.warn("CONFIG | STABILITYAI_API_KEY is unset or a placeholder — image generation will fail at runtime.");
        }
        if (!authEnabled) {
            log.warn("CONFIG | API-key authentication is DISABLED — every endpoint is open. Do not run this way in production.");
        }
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank() || value.startsWith("sk-placeholder") || value.startsWith("${");
    }
}
