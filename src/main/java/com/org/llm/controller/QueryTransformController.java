package com.org.llm.controller;

import com.org.llm.model.QueryTransformRequest;
import com.org.llm.model.QueryTransformResponse;
import com.org.llm.rag.QueryTransformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "Pre-retrieval query transformation playground (Spring AI modular RAG)")
class QueryTransformController {

    private final QueryTransformationService queryTransformationService;

    @Operation(summary = "Run a single query through one of Spring AI's pre-retrieval query "
            + "transformers (rewrite, translate, compress, multi-query-expand) and return the result")
    @PostMapping("/query-transform")
    public QueryTransformResponse transform(@Valid @RequestBody QueryTransformRequest request) {
        return new QueryTransformResponse(
                request.technique(),
                request.query(),
                queryTransformationService.transform(request));
    }
}
