package com.org.llm.config;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds the {@link WebClient} used to call {@code llm-gateway}. Servlet stack: the reactive
 * client is used for blocking calls (via {@code .block()}) and for SSE streaming.
 *
 * <p>The {@code X-Request-ID} from the current request's MDC context is propagated as
 * {@code X-Correlation-ID} on all outbound gateway calls for distributed tracing.</p>
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Bean
    WebClient gatewayWebClient(GatewayProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .filter(correlationIdFilter());

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.defaultHeader("X-API-Key", properties.getApiKey());
        }
        return builder.build();
    }

    /**
     * {@link ExchangeFilterFunction} that reads the current request's {@code requestId} from MDC
     * and forwards it as {@code X-Correlation-ID} on every outbound gateway call.
     */
    private static ExchangeFilterFunction correlationIdFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            String requestId = MDC.get(REQUEST_ID_MDC_KEY);
            if (requestId != null && !requestId.isBlank()) {
                ClientRequest mutated = ClientRequest.from(clientRequest)
                        .header(CORRELATION_ID_HEADER, requestId)
                        .build();
                return reactor.core.publisher.Mono.just(mutated);
            }
            return reactor.core.publisher.Mono.just(clientRequest);
        });
    }
}
