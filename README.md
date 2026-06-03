# LLM Chat — Spring AI Production-Grade Backend

A Spring AI **chat** service demonstrating production patterns: multi-turn chat with persistent
memory, streaming, audio (transcription / TTS / voice chat), image captioning + generation,
PDF/file reading and a guarded natural-language **text-to-SQL** endpoint — all behind API-key
auth, rate limiting and a full metrics/traces/logs observability stack.

> Sibling services: [`llm-gateway`](../llm-gateway) (multi-provider routing + guardrails) and
> [`llm-rag-pipeline`](../llm-rag-pipeline) (ingestion + retrieval). This repo follows the same
> security, observability and project conventions as those two.

## 🛠️ Technology Stack

- **Spring Boot** 4.0.6 · **Spring AI** 2.0.0-M8 · **Java** 21 · **Maven**
- **OpenAI** (chat, audio) · **Stability AI** (image generation)
- **PostgreSQL** — chat memory, contacts, text-to-SQL data, API keys
- **Redis** — vector store (for RAG-backed advisors)
- **Spring Security** — API-key authentication (`X-API-Key`) + in-memory rate limiting
- **Observability**: Micrometer + Prometheus + Grafana + Tempo (traces) + Loki (logs)

## 🏗️ Layout (`com.org.llm.*`)

- `controller/` — REST endpoints (chat, audio, image, file, recipe, text-to-sql).
- `service/` — Spring AI `ChatClient` interactions per capability.
- `security/` — API-key auth: `ApiKeyService` (SHA-256 hashes in the `api_keys` table),
  `ApiKeyAuthFilter`, `RateLimitFilter`, `SecurityConfig` (headers + CORS),
  `RestAuthenticationEntryPoint` (401 JSON).
- `exception/` — `GlobalExceptionHandler` + `ApiError` (consistent JSON error payloads).
- `config/` — `AIConfig` (ChatClient + advisors), `RedisConfig`, `ObservabilityConfig`
  (`@Timed`/`@Observed` aspects + JVM-extras), `StartupValidator` (fail-loud on missing keys).
- `validation/` — `SqlValidator` (read-only/allow-list SQL guard), `AudioValidator`.
- `common/Resilience` — tiny retry-with-backoff helper for transient outbound failures.
- `tool/` — Spring AI tools (weather, contacts).

## 🚀 Getting Started

### 1. Start infrastructure

```bash
docker compose up -d        # Postgres, Redis, RedisInsight + Prometheus/Grafana/Tempo/Loki
```

### 2. Configure secrets

```bash
export OPENAI_API_KEY=sk-...
export STABILITYAI_API_KEY=sk-...     # only for image generation
export WEATHER_API_KEY=...            # only for the weather tool
```

### 3. Run

```bash
./mvnw spring-boot:run
```

The app serves under context path **`/ai`** on port **8082** (e.g. http://localhost:8082/ai).

## 🔑 Authentication

API-key auth is **enabled by default**. Send the key in the `X-API-Key` header on every request
except actuator, the demo static pages and `/error`.

Flyway seeds a **development key** (`V5__create_api_keys.sql`):

```
X-API-Key: llm-chat-dev-key-2026
```

```bash
curl -s "http://localhost:8082/ai/recipe?ingredients=eggs,flour" \
  -H "X-API-Key: llm-chat-dev-key-2026"
```

Mint a real key:

```bash
raw=$(openssl rand -hex 32)
hash=$(printf "%s" "$raw" | shasum -a 256 | cut -d' ' -f1)
psql -h localhost -U postgres -d llm_chat \
  -c "INSERT INTO api_keys (key_hash, label) VALUES ('$hash', 'my-client');"
echo "X-API-Key: $raw"
```

To open everything for local development: `export API_AUTH_ENABLED=false` (or
`app.security.auth-enabled=false`). The demo HTML UIs under `/ai/*.html` assume an open
instance or that you inject the dev key.

## 🔀 Routing through llm-gateway

By default (`app.gateway.enabled=true`) chat, structured travel-guide and image generation are
routed through [`llm-gateway`](../llm-gateway) instead of calling OpenAI/Stability directly, so the
gateway owns provider keys, guardrails, failover and per-session memory:

| llm-chat flow                | Gateway call                          |
|------------------------------|---------------------------------------|
| `/chat`, audio chat          | `POST /llm/chat` (session = `conversationId`) |
| `/chat/stream`               | `POST /llm/{provider}/stream` (SSE)   |
| `/chat/travel-guide`         | `POST /llm/query` (strict-JSON → `TravelPlan`) |
| `/image/generate`            | `POST /llm/image` (OpenAI DALL·E)     |

Configure via `app.gateway.*` (`GATEWAY_ENABLED`, `GATEWAY_BASE_URL`, `GATEWAY_API_KEY`,
`GATEWAY_PROVIDER`, `GATEWAY_MODEL`, `GATEWAY_IMAGE_MODEL`). Set `GATEWAY_ENABLED=false` to call
the providers directly from this service (the original behaviour). Image captioning, audio
transcription/TTS and file reading always run locally — the gateway has no such endpoints.

Run order for the full setup: start `llm-gateway` (port 8080), then this service (port 8082).

## 📡 Endpoints (under `/ai`)

| Method | Path                  | Description                                    |
|--------|-----------------------|------------------------------------------------|
| POST   | `/chat`               | Multi-turn chat (memory via `conversationId`)  |
| POST   | `/chat/stream`        | Server-sent streaming chat                     |
| GET    | `/chat/memory`        | Inspect conversation memory                    |
| GET    | `/chat/travel-guide`  | Structured travel-guide response               |
| POST   | `/chat/audio`         | Chat with audio input                          |
| POST   | `/chat/audio/voice`   | Voice-to-voice chat                            |
| POST   | `/audio/to-text`      | Transcribe audio                               |
| POST   | `/audio/to-speech`    | Text-to-speech                                 |
| POST   | `/audio/upload`       | Upload + process an audio file                 |
| POST   | `/image/caption`      | Caption an image                               |
| GET    | `/image/generate`     | Generate an image (gateway DALL·E, or Stability if gateway off) |
| POST   | `/file/read`          | Read/summarise an uploaded file                |
| GET    | `/recipe`             | Generate a recipe from ingredients             |
| POST   | `/text-to-sql`        | NL → guarded read-only SQL + results           |

## 📊 Observability

See [`PROMETHEUS_GRAFANA_SETUP.md`](./PROMETHEUS_GRAFANA_SETUP.md). Health at
`/ai/actuator/health`, Prometheus scrape at `/ai/actuator/prometheus`, Grafana at
http://localhost:3000 (admin/admin) with the auto-provisioned **LLM Chat** dashboard.

## 🧱 Configuration

All tunables live in `application.yml` and accept environment overrides, e.g.
`SERVER_PORT`, `POSTGRES_*`, `REDIS_*`, `API_AUTH_ENABLED`, `RATE_LIMIT_ENABLED`,
`CORS_ALLOWED_ORIGINS`, `OTEL_EXPORTER_OTLP_ENDPOINT`.

## ✅ Build & Test

```bash
./mvnw verify        # compile, test, JaCoCo coverage report (target/site/jacoco)
```
