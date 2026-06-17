package com.org.llm.controller;

import com.org.llm.model.TextToSqlRequest;
import com.org.llm.model.TextToSqlResponse;
import com.org.llm.service.TextToSqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validation and {@code SqlValidationException} failures are translated to 400 JSON by
 * {@link com.org.llm.exception.GlobalExceptionHandler}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sql")
@Tag(name = "Text-to-SQL", description = "Natural language to SQL translation endpoints")
class TextToSqlController {

    private final TextToSqlService textToSqlService;

    @Operation(summary = "Translate a natural language question into an SQL query")
    @PostMapping
    public TextToSqlResponse textToSql(@Valid @RequestBody TextToSqlRequest request) {
        return textToSqlService.process(request);
    }
}
