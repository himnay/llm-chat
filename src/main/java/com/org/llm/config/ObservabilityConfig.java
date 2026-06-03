package com.org.llm.config;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer observation configuration.
 *
 * <p>Registers the {@link TimedAspect} (so {@code @Timed} methods become Prometheus timers) and
 * the {@link ObservedAspect} (so {@code @Observed} methods open tracing spans). Also exposes
 * process-level native memory/thread metrics consumed by the Grafana JVM dashboard.</p>
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

    /** Native process memory metrics ({@code process_memory_*}); Linux-only, no-op elsewhere. */
    @Bean
    MeterBinder processMemoryMetrics() {
        return new ProcessMemoryMetrics();
    }

    /** Process thread counts ({@code process_threads}); Linux-only, no-op elsewhere. */
    @Bean
    MeterBinder processThreadMetrics() {
        return new ProcessThreadMetrics();
    }
}
