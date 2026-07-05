package com.org.llm.controller;

import com.org.llm.backend.ImageBackend;
import com.org.llm.model.ImageCaptionRequest;
import com.org.llm.model.ImageGenerateRequest;
import com.org.llm.service.ImageCaptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public String caption(@Validated @RequestBody ImageCaptionRequest request) {
        return imageCaptionService.captionImage(request.imageName(), request.message());
    }

    @Operation(summary = "Generate a PNG image from a text prompt using Stability AI")
    @PostMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateImage(@Validated @RequestBody ImageGenerateRequest request) {
        byte[] png = imageBackend.generatePng(request.message(), request.style(), request.count());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
