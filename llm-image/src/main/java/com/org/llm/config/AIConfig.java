package com.org.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal {@code ChatClient} for {@code ImageCaptionService}'s multimodal (image + text) calls.
 * Captioning is single-shot — no conversation memory or safety advisors needed here.
 */
@Configuration
class AIConfig {

    /** Defines the chat client bean. */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
