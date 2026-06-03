package com.org.llm.controller;

import com.org.llm.model.ChatRequest;
import com.org.llm.service.FileReadService;
import com.org.llm.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
class FileRestController {

    private final FileReadService fileReadService;
    private final ImageGenerationService imageGenerationService;

    @PostMapping("/read")
    public String caption(@RequestBody ChatRequest chatRequest) {
        return fileReadService.readFile(chatRequest.imageName(), chatRequest.message());
    }
}
