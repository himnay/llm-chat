package com.org.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for routing LLM calls through {@code llm-gateway} (prefix {@code app.gateway}).
 *
 * <p>When {@link #enabled} is true, chat / structured-extraction / image generation are sent to
 * the gateway ({@code POST /llm/chat}, {@code /llm/query}, {@code /llm/image}) instead of calling
 * OpenAI / Stability directly, so the gateway owns provider keys, guardrails and failover.</p>
 */
@ConfigurationProperties(prefix = "app.gateway")
public class GatewayProperties {

    /** Route chat/structured/image traffic through the gateway. */
    private boolean enabled = true;

    /** Gateway base URL including its {@code /llm} base path. */
    private String baseUrl = "http://localhost:8080/llm";

    /** Raw API key for the gateway's {@code X-API-Key} header (only needed when the gateway has auth on). */
    private String apiKey = "";

    /** Provider the gateway should route to (openai | anthropic | ollama | google | huggingface | cohere). */
    private String provider = "openai";

    /** Optional chat model override; blank lets the gateway use its per-provider default. */
    private String model = "";

    /** Image model used by {@code POST /llm/image}. */
    private String imageModel = "dall-e-3";

    /** Per-call timeout (seconds) for blocking gateway requests. */
    private int timeoutSeconds = 60;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
