package com.org.llm.common;

/**
 * @deprecated Use Resilience4j {@code @CircuitBreaker} and {@code @Retry} annotations
 * on service methods instead. Retained only for binary-compatibility until all callers
 * are migrated.
 */
@Deprecated(since = "2.0", forRemoval = true)
public final class Resilience {

    private Resilience() {}
}
