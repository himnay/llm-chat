package com.org.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds the {@link WebClient} used to call {@code llm-gateway}. Servlet stack: the reactive
 * client is used for blocking calls (via {@code .block()}) and for SSE streaming.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    WebClient gatewayWebClient(GatewayProperties properties) {
        WebClient.Builder builder = WebClient.builder().baseUrl(properties.getBaseUrl());
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.defaultHeader("X-API-Key", properties.getApiKey());
        }
        return builder.build();
    }
}
