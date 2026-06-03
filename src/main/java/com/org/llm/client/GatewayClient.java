package com.org.llm.client;

import com.org.llm.client.dto.GatewayChatRequest;
import com.org.llm.client.dto.GatewayChatResponse;
import com.org.llm.client.dto.GatewayImageRequest;
import com.org.llm.client.dto.GatewayImageResponse;
import com.org.llm.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Thin client over {@code llm-gateway}'s HTTP API ({@code /llm/chat}, {@code /llm/query},
 * {@code /{provider}/stream}, {@code /llm/image}). Blocking calls use {@code .block()} with the
 * configured timeout; streaming returns the gateway's SSE token stream as a {@link Flux}.
 */
@Slf4j
@Component
public class GatewayClient {

    private final WebClient webClient;
    private final GatewayProperties properties;

    public GatewayClient(WebClient gatewayWebClient, GatewayProperties properties) {
        this.webClient = gatewayWebClient;
        this.properties = properties;
    }

    /** Multi-turn chat via {@code POST /llm/chat} (session-backed memory lives in the gateway). */
    public String chat(String systemPrompt, String userPrompt, String sessionId) {
        GatewayChatResponse response = webClient.post()
                .uri("/chat")
                .bodyValue(new GatewayChatRequest(userPrompt, properties.getProvider(),
                        modelOrNull(), systemPrompt, sessionId))
                .retrieve()
                .bodyToMono(GatewayChatResponse.class)
                .block(timeout());
        return extractText(response);
    }

    /** One-shot, stateless completion via {@code POST /llm/query} (used for structured extraction). */
    public String query(String systemPrompt, String userPrompt) {
        GatewayChatResponse response = webClient.post()
                .uri("/query")
                .bodyValue(new GatewayChatRequest(userPrompt, properties.getProvider(),
                        modelOrNull(), systemPrompt, null))
                .retrieve()
                .bodyToMono(GatewayChatResponse.class)
                .block(timeout());
        return extractText(response);
    }

    /** Streaming chat via {@code POST /llm/{provider}/stream} (Server-Sent Events of token chunks). */
    public Flux<String> streamChat(String userPrompt, String sessionId) {
        return webClient.post()
                .uri("/{provider}/stream", properties.getProvider())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(new GatewayChatRequest(userPrompt, properties.getProvider(),
                        modelOrNull(), null, sessionId))
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorResume(ex -> {
                    log.error("GATEWAY | stream failed | {}", ex.getMessage());
                    return Flux.error(new IllegalStateException("Gateway stream failed: " + ex.getMessage()));
                });
    }

    /** Image generation via {@code POST /llm/image}; returns the first image decoded from base64. */
    public byte[] generateImage(String prompt, Integer count) {
        GatewayImageResponse response = webClient.post()
                .uri("/image")
                .bodyValue(new GatewayImageRequest(prompt, properties.getImageModel(),
                        "1024x1024", count == null || count < 1 ? 1 : count, "b64_json"))
                .retrieve()
                .bodyToMono(GatewayImageResponse.class)
                .block(timeout());

        if (response == null || response.error() != null) {
            throw new IllegalStateException("Gateway image generation failed: "
                    + (response == null ? "no response" : response.error()));
        }
        List<String> images = response.images();
        if (images == null || images.isEmpty()) {
            throw new IllegalStateException("Gateway returned no image");
        }
        return Base64.getDecoder().decode(images.get(0));
    }

    private String extractText(GatewayChatResponse response) {
        if (response == null) {
            throw new IllegalStateException("Gateway returned no response");
        }
        if (response.error() != null) {
            throw new IllegalStateException("Gateway error: " + response.error());
        }
        return response.text();
    }

    private String modelOrNull() {
        return properties.getModel() == null || properties.getModel().isBlank() ? null : properties.getModel();
    }

    private Duration timeout() {
        return Duration.ofSeconds(properties.getTimeoutSeconds());
    }
}
