package com.org.llm.service;

import com.org.llm.client.GatewayClient;
import com.org.llm.config.GatewayProperties;
import com.org.llm.tool.ContactsTool;
import com.org.llm.tool.WeatherTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final WeatherTools weatherTools;
    private final ContactsTool contactsTool;
    private final GatewayClient gatewayClient;
    private final GatewayProperties gatewayProperties;

    public String chat(String conversationId, String message) {
        String convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String systemPrompt = "Today’s date is " + today + ". " +
                "You are a friendly travel guide. Suggest 3 attractions and 1 food item.";

        // When the gateway is enabled, route through llm-gateway (it owns provider keys,
        // guardrails, failover and per-session memory keyed by session_id).
        if (gatewayProperties.isEnabled()) {
            log.info("CHAT | routing via gateway | session={}", convId);
            return gatewayClient.chat(systemPrompt, message, convId);
        }

        // conversationId param is picked up by the default MessageChatMemoryAdvisor in AIConfig
        return chatClient.prompt()
                .system(systemPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, convId))
                .tools(weatherTools, contactsTool)
                .user(message)
                .call()
                .content();
    }

    public Flux<String> streamChat(String conversationId, String message) {
        String convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        // When the gateway is enabled, stream tokens from llm-gateway's SSE endpoint.
        if (gatewayProperties.isEnabled()) {
            log.info("CHAT | streaming via gateway | session={}", convId);
            return gatewayClient.streamChat(message, convId);
        }

        // QuestionAnswerAdvisor adds RAG context from vector store on top of the default memory advisor
        return chatClient.prompt()
                .advisors(spec -> spec
                        .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                        .param(ChatMemory.CONVERSATION_ID, convId))
                .tools(weatherTools, contactsTool)
                .user(message)
                .stream()
                .content()
                .doOnNext(s -> log.info("s : {}", s))
                .doOnComplete(() -> log.info("Data complete"))
                .onErrorResume(throwable -> {
                    log.error("Error occurred in the stream", throwable);
                    return Flux.error(new RuntimeException(
                            "Error occurred in the stream: %s"
                                    .formatted(throwable.getMessage())));
                });
    }

}
