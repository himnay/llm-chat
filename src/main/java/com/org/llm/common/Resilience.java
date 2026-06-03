package com.org.llm.common;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Minimal, dependency-free retry with exponential backoff for transient failures of outbound
 * calls (OpenAI chat / image / audio, Redis vector store). Deliberately tiny — for a full circuit
 * breaker, swap in Resilience4j once it ships a Spring Boot 4 starter.
 */
@Slf4j
public final class Resilience {

    private Resilience() {
    }

    public static <T> T withRetry(String operation, int maxAttempts, long baseBackoffMillis, Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                long backoff = baseBackoffMillis * (1L << (attempt - 1));
                log.warn("{} failed (attempt {}/{}): {} — retrying in {}ms",
                        operation, attempt, maxAttempts, ex.getMessage(), backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last;
    }

    /** Convenience for void actions. */
    public static void withRetry(String operation, int maxAttempts, long baseBackoffMillis, Runnable action) {
        withRetry(operation, maxAttempts, baseBackoffMillis, () -> {
            action.run();
            return null;
        });
    }
}
