package com.org.llm.controller;

import com.org.llm.model.FileReadRequest;
import com.org.llm.service.FileReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    /** Returns the caption. */
    @PostMapping("/read")
    @Operation(summary = "Read a document file and answer a question about its content")
    public String caption(@Valid @RequestBody FileReadRequest request) {
        return fileReadService.readFile(request.fileName(), request.message());
    }
}
