package com.infiproton.springaidemo.controller;

import com.infiproton.springaidemo.model.ChatRequest;
import com.infiproton.springaidemo.service.ImageCaptionService;
import com.infiproton.springaidemo.service.ImageGenerationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/image")
class ImageRestController {

    private final ImageCaptionService imageCaptionService;
    private final ImageGenerationService imageGenerationService;

    ImageRestController(ImageCaptionService imageCaptionService, ImageGenerationService imageGenerationService) {
        this.imageCaptionService = imageCaptionService;
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping("/caption")
    public String caption(@RequestBody ChatRequest chatRequest) {
        return imageCaptionService.captionImage(chatRequest.getImageName(), chatRequest.getMessage());
    }

    @GetMapping("/generate")
    public List<String> generateImage(@RequestParam String message,
                                      @RequestParam String style,
                                      @RequestParam Integer count) {
        return imageGenerationService.generate(message, style, count);
    }
}
