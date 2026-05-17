package com.infiproton.springaidemo.service;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageGenerationService {
    private final ImageModel imageModel;

    public ImageGenerationService(@Qualifier("stabilityAiImageModel") ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public List<String> generate(String message, String style, Integer count) {
        ImagePrompt prompt = new ImagePrompt(message,
                StabilityAiImageOptions.builder()
                        .stylePreset(style)
                        .N(count)
                        .responseFormat("b64_json")
                        .build());
        ImageResponse imageResponse = imageModel.call(prompt);

        return imageResponse.getResults().stream()
                .map(r -> "data:image/png;base64," + r.getOutput().getB64Json())
                .collect(Collectors.toList());
    }
}
