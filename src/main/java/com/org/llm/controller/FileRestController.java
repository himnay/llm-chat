package com.org.llm.controller;

import com.org.llm.model.ChatRequest;
import com.org.llm.service.FileReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "File reading and AI-assisted document Q&A endpoints")
class FileRestController {

    private final FileReadService fileReadService;

    @Operation(summary = "Read a document file and answer a question about its content")
    @PostMapping("/read")
    public String caption(@RequestBody ChatRequest chatRequest) {
        return fileReadService.readFile(chatRequest.imageName(), chatRequest.message());
    }
}
