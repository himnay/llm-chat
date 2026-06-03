package com.org.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class AIConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(
                        // Blocks requests containing forbidden phrases before any processing
                        SafeGuardAdvisor.builder()
                                .sensitiveWords(List.of("ignore previous instructions", "jailbreak", "prompt injection"))
                                .order(Integer.MIN_VALUE)
                                .build(),
                        // Enriches prompt with conversation history (conversationId set per-request via param)
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // Logs every request + response for observability (runs after memory enrichment)
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(50)
                .build();
    }
}
