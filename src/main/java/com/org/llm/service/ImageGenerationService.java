package com.org.llm.service;

import com.org.llm.client.GatewayClient;
import com.org.llm.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class ImageGenerationService {

    private final ImageModel imageModel;
    private final GatewayClient gatewayClient;
    private final GatewayProperties gatewayProperties;

    public ImageGenerationService(@Qualifier("stabilityAiImageModel") ImageModel imageModel,
                                  GatewayClient gatewayClient,
                                  GatewayProperties gatewayProperties) {
        this.imageModel = imageModel;
        this.gatewayClient = gatewayClient;
        this.gatewayProperties = gatewayProperties;
    }

    public ResponseEntity<byte[]> generate(String message, String style, Integer count) {
        // When the gateway is enabled, generate via llm-gateway (OpenAI DALL·E). The Stability
        // "style preset" has no DALL·E equivalent, so it's folded into the prompt instead.
        if (gatewayProperties.isEnabled()) {
            log.info("IMAGE | routing via gateway | style={} count={}", style, count);
            String prompt = (style == null || style.isBlank()) ? message : message + ", " + style + " style";
            byte[] png = gatewayClient.generateImage(prompt, count);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        }

        ImagePrompt prompt = new ImagePrompt(message,
                StabilityAiImageOptions.builder()
                        .stylePreset(style)
                        .N(count)
                        .responseFormat("b64_json")
                        .build());

        ImageResponse imageResponse = imageModel.call(prompt);

        byte[] png = Base64.getDecoder().decode(imageResponse.getResult().getOutput().getB64Json());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
