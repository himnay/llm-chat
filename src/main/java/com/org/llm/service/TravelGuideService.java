package com.org.llm.service;

import com.org.llm.client.GatewayClient;
import com.org.llm.config.GatewayProperties;
import com.org.llm.model.TravelPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelGuideService {

    /** JSON shape requested from the gateway so the free-form completion can be deserialized. */
    private static final String JSON_INSTRUCTION = """
            Respond with ONLY valid JSON (no markdown, no prose) matching exactly:
            {"city": string, "days": number, "itinerary": [{"day": number, "activities": string, "food": string, "budget": number}]}""";

    private final ChatClient chatClient;
    private final GatewayClient gatewayClient;
    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/travel-guide.st")
    private Resource travelGuideTemplate;

    public TravelPlan prepareTravelPlan(String city, Integer days) {
        PromptTemplate template = new PromptTemplate(travelGuideTemplate);

        // When the gateway is enabled, render the prompt, ask for strict JSON and deserialize
        // locally (the gateway returns free-form text, not a typed entity).
        if (gatewayProperties.isEnabled()) {
            log.info("TRAVEL | routing via gateway | city={} days={}", city, days);
            String rendered = template.render(Map.of("city", city, "days", days));
            String json = gatewayClient.query(JSON_INSTRUCTION, rendered);
            return parse(json);
        }

        // .entity() automatically appends JSON format instructions; no extra UserMessage needed
        return chatClient.prompt(template.create(Map.of("city", city, "days", days)))
                .call()
                .entity(TravelPlan.class);
    }

    private TravelPlan parse(String json) {
        try {
            return objectMapper.readValue(stripFences(json), TravelPlan.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Gateway returned a travel plan that could not be parsed: "
                    + ex.getMessage());
        }
    }

    /** Defensive: strip ```json fences if a model wraps the JSON despite instructions. */
    private static String stripFences(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("(?s)^```(?:json)?", "").replaceFirst("(?s)```$", "").trim();
        }
        return cleaned;
    }
}
