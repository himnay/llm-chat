package com.org.llm.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RequestIdFilter}.
 */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String responseId = response.getHeader("X-Request-ID");
        assertThat(responseId).isNotBlank();
        // Should be a valid UUID
        assertThat(responseId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void propagatesRequestIdWhenHeaderPresent() throws ServletException, IOException {
        String existingId = "my-custom-request-id-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isEqualTo(existingId);
    }

    @Test
    void clearsMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            // requestId should be in MDC during chain execution
            assertThat(MDC.get("requestId")).isNotBlank();
        };

        filter.doFilterInternal(request, response, chain);

        // MDC must be cleared after the filter completes
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void setsRequestIdInMdcDuringChain() throws ServletException, IOException {
        String requestId = "trace-abc-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) ->
                assertThat(MDC.get("requestId")).isEqualTo(requestId);

        filter.doFilterInternal(request, response, chain);
    }
}
