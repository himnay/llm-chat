-- API keys for authenticating REST clients (X-API-Key header).
-- Raw keys are NEVER stored — only their SHA-256 hex digest (see ApiKeyService).
-- pgcrypto provides digest() so the dev key below can be hashed inline.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE api_keys (
    id          BIGSERIAL   PRIMARY KEY,
    key_hash    VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex of the raw key
    label       VARCHAR(100),                  -- human-friendly name for the key's owner
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    expires_at  TIMESTAMPTZ,                   -- NULL = never expires
    last_used   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_key_hash ON api_keys (key_hash);

-- Seed a development key so the API and demo UIs are usable out of the box.
-- Raw key (LOCAL DEV ONLY — rotate/remove in production):  llm-chat-dev-key-2026
INSERT INTO api_keys (key_hash, label, enabled)
VALUES (encode(digest('llm-chat-dev-key-2026', 'sha256'), 'hex'), 'Development Key', TRUE);

-- To mint another key:
--   raw=$(openssl rand -hex 32)
--   hash=$(printf "%s" "$raw" | shasum -a 256 | cut -d' ' -f1)
--   INSERT INTO api_keys (key_hash, label) VALUES ('<hash>', 'my-client');
-- then send the RAW value in the X-API-Key header.
