package com.org.llm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FileReadRequest(
        @NotBlank(message = "fileName is required")
        // A plain PDF name under classpath:files/ — no path separators, so "../application.yaml"
        // can't read other classpath resources and send them to the model.
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*\\.pdf", message = "fileName must be a PDF file name such as policy.pdf")
        String fileName,
        @NotBlank(message = "message is required")
        String message
) {
}
