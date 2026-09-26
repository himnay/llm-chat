package com.org.llm.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FileReadRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("A plain PDF name under files/ is valid")
    void acceptsPlainPdfName() {
        assertThat(validator.validate(new FileReadRequest("policy.pdf", "summarize"))).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Paths and non-PDF names are rejected, so no other classpath resource can be read")
    @ValueSource(strings = {"../application.yaml", "../application.pdf", "files/policy.pdf", "..\\secret.pdf",
            ".hidden.pdf", "policy.txt"})
    void rejectsPathsAndNonPdfNames(String fileName) {
        assertThat(validator.validate(new FileReadRequest(fileName, "summarize"))).isNotEmpty();
    }
}
