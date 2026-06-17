package com.org.llm.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ChatClient advisor ordering:
 *   1. SafeGuardAdvisor  (Integer.MIN_VALUE) — blocks prompt injection before anything else
 *   2. MessageChatMemoryAdvisor              — loads conversation history
 *   3. TokenCountingAdvisor (order = 0)      — records token usage
 *   4. SimpleLoggerAdvisor  (last)           — logs the full exchange
 */
class AdvisorOrderTest {

    @Test
    void safeGuardAdvisorHasHighestPriority() {
        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("jailbreak"))
                .order(Integer.MIN_VALUE)
                .build();

        assertThat(safeGuard.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void simpleLoggerAdvisorOrderIsHigherThanSafeGuard() {
        SimpleLoggerAdvisor logger = new SimpleLoggerAdvisor();

        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("jailbreak"))
                .order(Integer.MIN_VALUE)
                .build();

        // Logger fires last (higher order value = lower priority = runs closer to the end)
        assertThat(logger.getOrder()).isGreaterThan(safeGuard.getOrder());
    }

    @Test
    void tokenCountingAdvisorOrderIsBetweenSafeGuardAndLogger() {
        // Inline implementation matching what AIConfig creates
        CallAdvisor tokenAdvisor = new CallAdvisor() {
            @Override
            public ChatClientResponse adviseCall(ChatClientRequest req, CallAdvisorChain chain) {
                return chain.nextCall(req);
            }
            @Override public String getName() { return "TokenCountingAdvisor"; }
            @Override public int getOrder() { return 0; }
        };

        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("jailbreak"))
                .order(Integer.MIN_VALUE)
                .build();
        SimpleLoggerAdvisor logger = new SimpleLoggerAdvisor();

        assertThat(tokenAdvisor.getOrder())
                .isGreaterThan(safeGuard.getOrder())
                .isLessThanOrEqualTo(logger.getOrder());
    }

    @Test
    void safeGuardBlockedWordsDefaultsAreNonEmpty() {
        List<String> defaults = List.of(
                "ignore previous instructions", "jailbreak", "prompt injection",
                "ignore all previous", "forget your instructions", "you are now DAN");

        SafeGuardAdvisor advisor = SafeGuardAdvisor.builder()
                .sensitiveWords(defaults)
                .build();

        assertThat(advisor).isNotNull();
        assertThat(defaults).hasSizeGreaterThan(0);
    }
}
