package com.org.llm.backend;

import com.org.llm.client.GatewayClient;
import com.org.llm.model.ChatAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Routes chat through {@code llm-gateway} (it owns provider keys, guardrails, failover and
 * per-session memory keyed by the conversation id). The gateway has no RAG integration, so this
 * backend never produces citations or a faithfulness verdict.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayChatBackend implements ChatBackend {

    private final GatewayClient gatewayClient;

    @Override
    public ChatAnswer chat(String systemPrompt, String conversationId, String message) {
        log.info("CHAT | routing via gateway | session={}", conversationId);
        String answer = gatewayClient.chat(systemPrompt, message, conversationId);
        return ChatAnswer.withoutRag(answer);
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(String conversationId, String message) {
        log.info("CHAT | streaming via gateway | session={}", conversationId);
        Flux<ServerSentEvent<String>> tokens = gatewayClient.streamChat(message, conversationId)
                .map(token -> ServerSentEvent.<String>builder().event("token").data(token).build());
        return tokens.concatWith(Flux.just(
                ServerSentEvent.<String>builder().event("citations").data("[]").build()));
    }
}
