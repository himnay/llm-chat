package com.org.llm.controller;

import com.org.llm.backend.ImageBackend;
import com.org.llm.model.ChatRequest;
import com.org.llm.service.ImageCaptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Tag(name = "Images", description = "Image captioning and AI image generation endpoints")
class ImageRestController {

    private final ImageCaptionService imageCaptionService;
    private final ImageBackend imageBackend;

    @Operation(summary = "Generate a text caption for a named image")
    @PostMapping("/caption")
    public String caption(@Validated @RequestBody ChatRequest chatRequest) {
        return imageCaptionService.captionImage(chatRequest.imageName(), chatRequest.message());
    }

    @Operation(summary = "Generate a PNG image from a text prompt using Stability AI")
    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateImage(
            @NotBlank(message = "message is required") @RequestParam String message,
            @NotBlank(message = "style is required") @RequestParam String style,
            @Positive(message = "count must be a positive number") @RequestParam Integer count) {
        byte[] png = imageBackend.generatePng(message, style, count);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
