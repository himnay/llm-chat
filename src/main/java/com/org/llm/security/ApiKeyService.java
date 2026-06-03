package com.org.llm.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * API-key validation backed by PostgreSQL (JdbcTemplate).
 *
 * <p>Raw keys are never stored — only their SHA-256 hex digest. Mirrors the
 * {@code llm-gateway} / {@code llm-rag-pipeline} key model ({@code key_hash},
 * {@code enabled}, {@code expires_at}, {@code last_used}).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final JdbcTemplate jdbc;

    /** True if the raw key maps to an enabled, non-expired row. Never throws. */
    public boolean isValid(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }
        String hash = sha256(rawKey.trim());
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM api_keys
                     WHERE key_hash = ?
                       AND enabled = TRUE
                       AND (expires_at IS NULL OR expires_at > NOW())
                    """, Integer.class, hash);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            log.error("API_KEY | DB lookup failed | error={}", ex.getMessage());
            return false;
        }
    }

    /** Best-effort {@code last_used} stamp; failures are swallowed (never blocks the request). */
    public void touchLastUsed(String rawKey) {
        try {
            jdbc.update("UPDATE api_keys SET last_used = NOW() WHERE key_hash = ?", sha256(rawKey.trim()));
        } catch (DataAccessException ex) {
            log.warn("API_KEY | last_used update failed | error={}", ex.getMessage());
        }
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
