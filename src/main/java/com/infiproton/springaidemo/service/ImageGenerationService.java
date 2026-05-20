package com.infiproton.springaidemo.service;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageGenerationService {

    private final ImageModel imageModel;

    public ImageGenerationService(@Qualifier("stabilityAiImageModel") ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public ResponseEntity<byte[]> generate(String message, String style, Integer count) {
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
