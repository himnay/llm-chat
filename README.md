# LLM Chat — Spring AI Production-Grade Backend

A Spring AI **chat** service demonstrating production patterns: multi-turn chat with persistent
memory, streaming, audio (transcription / TTS / voice chat), image captioning + generation,
PDF/file reading and a guarded natural-language **text-to-SQL** endpoint — all behind API-key
auth, rate limiting and a full metrics/traces/logs observability stack.

> Sibling services: [`llm-gateway`](../llm-gateway) (multi-provider routing + guardrails) and
> [`llm-rag-pipeline`](../llm-rag-pipeline) (ingestion + retrieval). This repo follows the same
> security, observability and project conventions as those two.

## 🛠️ Technology Stack

- **Spring Boot** 4.1.0 · **Spring AI** 2.0.0 · **Java** 25 · **Maven**
- **OpenAI** (chat, audio) · **Stability AI** (image generation)
- **PostgreSQL** — chat memory, contacts, text-to-SQL data, API keys
- **Redis** — vector store (for RAG-backed advisors)
- **Spring Security** — API-key authentication (`X-API-Key`) + in-memory rate limiting
- **Observability**: Micrometer + Prometheus + Grafana + Tempo (traces) + Loki (logs)

## 🏗️ Layout (`com.org.llm.*`)

- `controller/` — REST endpoints (chat, audio, image, file, recipe, text-to-sql).
- `service/` — application layer per capability (`ChatService`, `TravelGuideService`,
  `AudioService`, `VoiceChatService` — a facade over the validate → store → transcribe →
  chat → synthesize pipeline, `TextToSqlService`, …).
- `backend/` — **Strategy** pattern for where LLM work executes: `ChatBackend`,
  `TravelPlanBackend` and `ImageBackend` each have a `Gateway*` and a `Local*` implementation;
  exactly one is active per run, selected at startup by `app.gateway.enabled`
  (`@ConditionalOnProperty`).
- `security/` — API-key auth: `ApiKeyService` (SHA-256 hashes in the `api_keys` table),
  `ApiKeyAuthFilter`, `RateLimitFilter`, `SecurityConfig` (headers + CORS),
  `RestAuthenticationEntryPoint` (401 JSON).
- `exception/` — `GlobalExceptionHandler` + `ApiError` (consistent JSON error payloads).
  Custom hierarchy: `ValidationException` → `AudioValidationException` (audio file checks),
  `SqlValidationException` (SQL guard checks). Each maps to HTTP 400 with a distinct error label.
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

- API-key auth is **enabled by default** — every request must include `X-API-Key` in the header
- Excluded from auth: actuator endpoints, the demo static HTML pages, and `/error`
- Keys are stored as SHA-256 hashes in the `api_keys` PostgreSQL table — raw values are never persisted
- Flyway seeds a **development key** (`V5__create_api_keys.sql`) ready to use immediately:

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

- To disable auth for local development: set `API_AUTH_ENABLED=false` (or `app.security.auth-enabled=false`)
- The demo HTML UIs under `/ai/*.html` assume an open instance or that you inject the dev key directly

## 🔀 Routing through llm-gateway

- By default (`app.gateway.enabled=true`), chat, structured travel-guide, and image generation are **routed through `llm-gateway`** rather than calling OpenAI/Stability directly
- The gateway owns provider API keys, guardrails, failover logic, and per-session memory — centralising those concerns outside this service
- When `GATEWAY_ENABLED=false`, this service calls providers directly (the original behaviour)
- Image captioning, audio transcription/TTS, and file reading **always run locally** — the gateway exposes no such endpoints

| llm-chat flow                | Gateway call                          |
|------------------------------|---------------------------------------|
| `/chat`, audio chat          | `POST /llm/chat` (session = `conversationId`) |
| `/chat/stream`               | `POST /llm/{provider}/stream` (SSE)   |
| `/chat/travel-guide`         | `POST /llm/query` (strict-JSON → `TravelPlan`) |
| `/image/generate`            | `POST /llm/image` (OpenAI DALL·E)     |

- Configure via `app.gateway.*` env vars: `GATEWAY_ENABLED`, `GATEWAY_BASE_URL`, `GATEWAY_API_KEY`, `GATEWAY_PROVIDER`, `GATEWAY_MODEL`, `GATEWAY_IMAGE_MODEL`
- Recommended run order for the full stack: start `llm-gateway` on port 8080 first, then this service on port 8082

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
| POST   | `/rag/query-transform` | Run a query through a single pre-retrieval transformer (rewrite/translate/compress/multi-query-expand) |

## 📊 Observability

See [`PROMETHEUS_GRAFANA_SETUP.md`](./PROMETHEUS_GRAFANA_SETUP.md). Health at
`/ai/actuator/health`, Prometheus scrape at `/ai/actuator/prometheus`, Grafana at
http://localhost:3000 (admin/admin) with the auto-provisioned **LLM Chat** dashboard.

### Actuator endpoints

| Endpoint | Description |
|---|---|
| `/ai/actuator/health` | Full component health (DB, Redis, liveness/readiness probes) |
| `/ai/actuator/info` | Build info (version, time), git info (branch, commit, dirty flag), Java/OS details |
| `/ai/actuator/metrics` | Micrometer metrics |
| `/ai/actuator/prometheus` | Prometheus scrape target |

- `/actuator/info` is enriched at build time by `spring-boot-maven-plugin` (`build-info` goal) and `git-commit-id-maven-plugin`
- Run `./mvnw package` to populate build timestamp, version, and Git commit details into the info endpoint

## 🧱 Configuration

- All tunables live in `application.yml` and accept environment variable overrides at runtime
- Key environment variables: `SERVER_PORT`, `POSTGRES_*`, `REDIS_*`, `API_AUTH_ENABLED`, `RATE_LIMIT_ENABLED`, `CORS_ALLOWED_ORIGINS`, `OTEL_EXPORTER_OTLP_ENDPOINT`
- No rebuild required — all knobs are externalised and take effect on the next startup

## ✅ Build & Test

```bash
./mvnw verify        # compile, test, JaCoCo coverage report (target/site/jacoco)
```

- Integration tests use **Testcontainers** (`TestcontainersConfiguration` + `@ServiceConnection`)
- A throwaway `postgres:18` container is started per test run — no locally provisioned database needed, only Docker
- Flyway migrations, the JDBC chat-memory schema, and all JDBC queries in tests run against the real Postgres 18 container
- Validator logic (`SqlValidator`, `AudioValidator`) is covered by plain unit tests with no container dependency

## Technology Deep Dive

This section explains every significant library, framework, database, and infrastructure component used in this project — what it is and exactly how it is wired up here.

---

### Spring Boot 4.1.0

**What it is.**

- Spring Boot is an opinionated framework that auto-configures a production-ready Java application from a single `main` class and a classpath of starter JARs
- Version 4.x requires Java 17+ and brings the `jakarta.*` namespace (Jakarta EE 11); this project runs on Java 25
- Auto-configuration is now further modularised — each technology ships its own auto-config module rather than bundling everything in one jar

**How it's used here.**

- Entry point is `LLMApplication`; `spring-boot-starter-web` stands up a Tomcat servlet container on port 8082 under context path `/ai`
- `spring-boot-starter-validation` enables `@Valid` on controller method parameters for automatic request-body validation
- `server.shutdown: graceful` with a 30-second drain window ensures rolling deployments do not drop in-flight requests
- The Spring Boot Maven plugin is configured with the `build-info` goal so `/ai/actuator/info` reports build timestamp, version, and Git commit

---

### Spring AI 2.0.0

**What it is.**

- Spring AI is the official Spring integration layer for large-language models and AI services
- Provides a provider-neutral `ChatClient` abstraction, chat-memory advisors, document readers, vector store abstractions, audio model wrappers, image model wrappers, and a `@Tool` annotation for function calling
- All components follow standard Spring conventions — dependency injection, auto-configuration, `application.yml` properties

**How it's used here.**

- **`ChatClient`** (configured in `AIConfig`) is built from `OpenAiChatModel` and pre-loaded with three default advisors:
  - `SafeGuardAdvisor` — blocks known jailbreak phrases before any processing
  - `MessageChatMemoryAdvisor` — enriches every prompt with per-conversation history retrieved from PostgreSQL
  - `SimpleLoggerAdvisor` — logs request/response pairs for observability
- **`JdbcChatMemoryRepository` + `MessageWindowChatMemory`** persist conversation history to PostgreSQL and cap the window at 50 messages so memory survives restarts
- **`OpenAiAudioTranscriptionModel` / `OpenAiAudioSpeechModel`** are injected directly into `AudioService` to call Whisper (speech-to-text) and TTS-1 (text-to-speech) via Spring AI's audio abstraction
- **`spring-ai-pdf-document-reader`, `spring-ai-markdown-document-reader`, `spring-ai-tika-document-reader`** are available for the file-reading and RAG-backed advisor flows
- **`spring-ai-starter-vector-store-redis`** wires a Redis vector store that the RAG advisor can query for context from uploaded PDFs
- **`spring-ai-rag`** provides the modular RAG building blocks — `RetrievalAugmentationAdvisor`, `VectorStoreDocumentRetriever`, `ContextualQueryAugmenter`, and the pre-retrieval query transformers/expander (`CompressionQueryTransformer`, `RewriteQueryTransformer`, `TranslationQueryTransformer`, `MultiQueryExpander`) — used to make retrieval history-aware and exposed individually via the query-transformation playground (see below)
- **`PromptTemplate`** loads the `travel-guide.st` StringTemplate file and fills `{city}` / `{days}` placeholders before passing the prompt to the travel-plan backend
- **`@Tool`** on `WeatherTools.getWeather` and `ContactsTool.findContactsByCity` registers those methods as callable functions the LLM can invoke during a chat turn

**Advisor chain ordering**

The three default advisors fire in a fixed order on every `ChatClient` call:

| Order | Advisor | Role |
|---|---|---|
| `Integer.MIN_VALUE` | `SafeGuardAdvisor` | Runs first — blocks jailbreak / prompt-injection phrases before any memory lookup or model call is made |
| default | `MessageChatMemoryAdvisor` | Fetches the last 50 messages for the `conversationId` from PostgreSQL and prepends them to the prompt as `USER`/`ASSISTANT` turns |
| default | `SimpleLoggerAdvisor` | Logs full request and response pairs at `DEBUG` level for observability; runs last so it captures the fully assembled prompt |

`SafeGuardAdvisor.order(Integer.MIN_VALUE)` guarantees the guard fires before memory is even consulted — a blocked request never touches the database.

**History-aware RAG on the streaming endpoint**

`LocalChatBackend.stream()` attaches a `RetrievalAugmentationAdvisor` on every streaming call (configured in `AIConfig`):

```java
chatClient.prompt()
    .advisors(spec -> spec
            .advisors(retrievalAugmentationAdvisor)
            .param(ChatMemory.CONVERSATION_ID, conversationId))
    .stream()
    ...
```

Multi-turn messages ("what about the second one?") have no standalone meaning, so embedding them as-is against the vector store returns poor matches. `AIConfig` wires the advisor as a small **retrieve → filter → augment → generate** pipeline instead of a single opaque step:

```java
@Bean
public QueryTransformer compressionQueryTransformer(OpenAiChatModel openAiChatModel) {
    // Temperature 0 keeps the standalone-query rewrite deterministic; a separate
    // ChatClient.Builder so it never touches the main conversation's options.
    ChatClient.Builder compressionClientBuilder = ChatClient.builder(openAiChatModel)
            .defaultOptions(OpenAiChatOptions.builder().temperature(0.0));
    return CompressionQueryTransformer.builder()
            .chatClientBuilder(compressionClientBuilder)
            .build();
}

@Bean
public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
                                                                   QueryTransformer compressionQueryTransformer) {
    return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(compressionQueryTransformer)
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .build())
            .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
            .build();
}
```

Because `RetrievalAugmentationAdvisor` is added per-call *after* the chat client's default `MessageChatMemoryAdvisor`, the prompt it sees already has the last 50 messages for the conversation prepended. `CompressionQueryTransformer` folds that history plus the current message into one standalone query with an LLM call (e.g. *"what about the second one?"* → *"What are the details of the second leave type in NexaCorp's leave policy?"*), `VectorStoreDocumentRetriever` embeds **that** compressed query and searches the Redis index (pre-loaded from `AtlasCorp-TravelPolicy.pdf` and `AtlasCorp_Events_Holidays.pdf`), and `ContextualQueryAugmenter` injects the retrieved chunks as a `SYSTEM` context block before the first token is streamed.

Two things worth calling out:
- Compression only changes what's sent to the retriever — the user's original message text is still what gets stored in `ChatMemory`, so conversation history isn't silently rewritten.
- `allowEmptyContext(true)` keeps the advisor permissive: if retrieval finds nothing relevant (e.g. small talk), the model still answers instead of refusing.

**Conversation ID flow**

Each chat request passes `conversationId` through Spring AI's advisor parameter map:

```java
spec.param(ChatMemory.CONVERSATION_ID, conversationId)
```

No per-request state lives in any Spring bean — the ID travels through `AdvisedRequest` metadata and is read by `MessageChatMemoryAdvisor` to fetch and save the correct message window. This is safe for concurrent requests with different conversation IDs on the same `ChatClient` instance.

**Query transformation playground**

`CompressionQueryTransformer` is only one of several pre-retrieval query transformers Spring AI ships under `spring-ai-rag`. `POST /api/v1/rag/query-transform` exposes all four so each can be exercised independently of the main chat flow:

| Technique | Spring AI class | What it does |
|---|---|---|
| `REWRITE` | `RewriteQueryTransformer` | Rewrites a messy/conversational query into a clean, standalone search query |
| `TRANSLATE` | `TranslationQueryTransformer` | Translates the query into the language of the indexed documents (defaults to English) |
| `COMPRESS` | `CompressionQueryTransformer` | Folds conversation history + the current query into one standalone query |
| `MULTI_QUERY_EXPAND` | `MultiQueryExpander` | Generates several paraphrased variants of the query to improve recall |

```json
POST /api/v1/rag/query-transform
{
  "technique": "COMPRESS",
  "query": "what about the second one?",
  "history": ["What is the capital of Denmark?", "Copenhagen is the capital of Denmark."]
}
```

```json
{
  "technique": "COMPRESS",
  "originalQuery": "what about the second one?",
  "transformedQueries": ["What is the second largest city in Denmark?"]
}
```

**Design — Strategy pattern.** Each technique is a `com.org.llm.rag.QueryTransformationStrategy` bean (`RewriteQueryStrategy`, `TranslateQueryStrategy`, `CompressQueryStrategy`, `MultiQueryExpansionStrategy`), tagged by a `QueryTransformationTechnique` enum value. `QueryTransformationService` collects all of them via constructor injection (`List<QueryTransformationStrategy>`) into a `Map<QueryTransformationTechnique, QueryTransformationStrategy>` and dispatches each request to the matching one — no `if`/`switch` chain, and adding a fifth technique (e.g. HyDE or step-back prompting) only means adding one more `@Component`, nothing else changes. The four underlying transformer/expander beans live in `RagConfig`, built from a shared low-temperature `ChatClient.Builder` (`ragChatClientBuilder`) so transformation calls stay deterministic and never touch the main conversation's options. `RewriteQueryTransformer` and `MultiQueryExpander` are configured once as singletons; `TranslationQueryTransformer` is built per-request inside its strategy because `targetLanguage` varies per call.

This endpoint is a playground for inspecting each technique's raw output — it does not itself touch the vector store. The production retrieval path (`LocalChatBackend.stream()`) only wires in `CompressionQueryTransformer`, via the `RetrievalAugmentationAdvisor` bean described above.

---

### OpenAI API

**What it is.**

- OpenAI provides GPT-series chat-completion models, the Whisper speech-recognition model, TTS voice-synthesis models, and DALL·E image-generation
- Access is REST-based, authenticated with a bearer token supplied as `OPENAI_API_KEY`

**How it's used here.**

- When `app.gateway.enabled=false` (direct mode), `LocalChatBackend` calls OpenAI's chat-completion API via `ChatClient`
- `LocalImageBackend` calls the image endpoint via the Stability AI or OpenAI image model
- `AudioService` calls Whisper with model `whisper-1` for transcription and `tts-1` with voice `echo` for speech synthesis
- The API key is read from `OPENAI_API_KEY` and kept only in Spring AI auto-configuration — it never appears in business code
- `StartupValidator` fails the application at boot if the key is missing, giving an immediate and unambiguous error

---

### Stability AI

**What it is.**

- Stability AI offers image-generation REST APIs including Stable Diffusion XL
- Spring AI bundles a `spring-ai-starter-model-stability-ai` dependency that wraps the HTTP calls into a typed model interface

**How it's used here.**

- `LocalImageBackend` calls `StabilityAiImageModel` to generate images when the gateway is disabled
- The configured model is `stable-diffusion-xl-1024-v1-0`, set in `application.yml` under `spring.ai.stabilityai.image.options.model`
- When the gateway is enabled, image generation is re-routed through `GatewayImageBackend`, which calls the gateway's `/llm/image` endpoint requesting DALL·E 3 instead

---

### PostgreSQL 18

**What it is.**

- PostgreSQL is an open-source relational database; version 18 stores data in a versioned sub-directory inside the data volume, which is why the Docker volume mounts the parent path `/var/lib/postgresql`

**How it's used here.** The database (`llm_chat`) holds five concerns managed entirely through Flyway migrations:

| Table | Purpose |
|---|---|
| `spring_ai_chat_memory` | Chat history rows written by `JdbcChatMemoryRepository` (auto-created by Spring AI) |
| `contacts` | Seed data for the contacts tool (`ContactsTool.findContactsByCity`) |
| `text2sql_customers / products / orders / order_items` | Demo e-commerce schema the text-to-SQL endpoint queries |
| `api_keys` | SHA-256 hashes of REST API keys validated by `ApiKeyService` |

- `JdbcTemplate` (no ORM) is used for all custom SQL: key lookups, contacts queries, text-to-SQL execution, and schema introspection at runtime
- The `pgcrypto` extension is enabled in migration V5 to hash the development seed key inline

---

### Flyway

**What it is.**

- Flyway is a database-migration tool that tracks which SQL scripts have been applied via a version-history table and runs any pending ones at application startup
- Prevents schema drift between environments by making migrations automatic and auditable

**How it's used here.**

- Five versioned scripts (`V1` through `V5`) under `src/main/resources/db/migration` set up all tables and seed data
- A separate history table (`flyway_schema_history_chat`) is used — distinct from the gateway and RAG services — so all three sibling services can share the same Postgres instance without migration-history conflicts
- `baseline-on-migrate: true` allows Flyway to adopt an already-initialised schema on first run without failing
- Spring Boot 4 requires the `spring-boot-flyway` module explicitly on the classpath — `flyway-core` alone no longer triggers `FlywayAutoConfiguration`

---

### Redis

**What it is.**

- Redis is an in-memory data structure store used here in two distinct roles: as a persistent key-value / list store with AOF durability, and as a vector database via the RediSearch module

**How it's used here.**

- **Vector store**: `spring-ai-starter-vector-store-redis` initialises a vector index on startup (`initialize-schema: true`) so the RAG advisor can embed and retrieve document chunks from uploaded PDFs (e.g., `AtlasCorp-TravelPolicy.pdf`)
- **Connection**: `RedisConfig` creates a `JedisConnectionFactory` (using the Jedis client) pointing at `localhost:6379` by default, overridable via `REDIS_HOST` / `REDIS_PORT`
- **Docker**: Redis is started with `appendonly yes` (AOF persistence), a 512 MB memory cap, and an `allkeys-lru` eviction policy so vector data survives restarts and old entries are evicted gracefully under memory pressure
- **RedisInsight**: A companion `redis/redisinsight` container (port 5540) provides a browser UI for inspecting vector-store contents during development

---

### Spring Security

**What it is.**

- Spring Security is the standard authentication and authorisation framework for Spring applications
- Works through a chain of servlet filters that intercept every request before it reaches a controller

**How it's used here.** The project implements a custom, database-backed API-key scheme instead of session or JWT:

1. **`ApiKeyAuthFilter`** reads the `X-API-Key` header, calls `ApiKeyService.isValid`, and if valid sets a `PreAuthenticatedAuthenticationToken` in the `SecurityContextHolder`; also calls `touchLastUsed` to stamp the row
2. **`ApiKeyService`** hashes the raw key with SHA-256 (using `MessageDigest`) and checks the `api_keys` table via `JdbcTemplate` — raw keys are never stored
3. **`RateLimitFilter`** applies a token-bucket rate limiter (120 requests/minute burst) per API key (or client IP when no key is present); the bucket is an in-memory `ConcurrentHashMap` of hand-rolled `Bucket` objects — no Resilience4j or Bucket4j dependency; returns `429` with a JSON `ApiError` on exhaustion
4. **`SecurityConfig`** declares which paths are open (actuator, static HTML, `/error`) and which require authentication; also configures CORS (`CORS_ALLOWED_ORIGINS`) and security headers
5. **`RestAuthenticationEntryPoint`** returns a structured JSON `{"status":401,...}` error rather than the default HTML challenge page
- Auth can be fully disabled for local development via `API_AUTH_ENABLED=false`

---

### Micrometer + Prometheus

**What it is.**

- Micrometer is the metrics-instrumentation facade for the JVM — analogous to SLF4J for logging
- Prometheus is a time-series metrics database that scrapes HTTP endpoints on a configurable interval

**How it's used here.**

- `spring-boot-starter-actuator` exposes `/ai/actuator/prometheus` as a Prometheus-format text scrape target
- `micrometer-registry-prometheus` registers the Prometheus `MeterRegistry` with Spring Boot
- `ObservabilityConfig` registers two AOP aspects: `TimedAspect` (activates `@Timed` on service methods to create histograms) and `ObservedAspect` (activates `@Observed` to open tracing spans)
- `micrometer-jvm-extras` adds process-level native memory and thread-count metrics (`process_memory_*`, `process_threads`)
- HTTP SLO buckets are configured at 50ms, 100ms, 200ms, 300ms, 500ms, 1s, 2s, and 5s for the `http.server.requests` histogram, enabling percentile and SLO dashboards in Grafana
- Prometheus scrapes the app every 10 seconds via `observability/prometheus.yml`

---

### Grafana

**What it is.**

- Grafana is a dashboarding and visualisation platform that can query Prometheus (metrics), Tempo (traces), and Loki (logs) from a single UI
- Supports alerting, annotations, and templated dashboards shareable as JSON

**How it's used here.**

- A `grafana/grafana` container (port 3000, admin/admin) is provisioned at startup via files under `observability/grafana/provisioning/`
- Prometheus, Tempo, and Loki are auto-configured as data sources — no manual datasource setup required
- The pre-built **LLM Chat** dashboard is loaded automatically so the full observability picture is available immediately after `docker compose up`
- Grafana depends on all three backend services in the Docker Compose definition, so startup ordering is correct

---

### Grafana Tempo

**What it is.**

- Tempo is a distributed tracing backend that stores and retrieves OpenTelemetry (OTLP) traces
- Designed to be cost-efficient by storing traces on local or object storage without a separate index

**How it's used here.**

- The app exports traces over OTLP HTTP to `http://localhost:4318/v1/traces` at 100% sampling, configurable via `OTEL_EXPORTER_OTLP_ENDPOINT`
- `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` wire Micrometer's observation API into OpenTelemetry's SDK, which ships spans to Tempo
- `traceId` and `spanId` from the MDC are included in every log line (both console and JSON) so log lines in Loki can be correlated directly to traces in Tempo
- Tempo is configured in `observability/tempo.yml` with OTLP gRPC (port 4317) and HTTP (port 4318) receivers and 24-hour local trace retention

---

### Grafana Loki

**What it is.**

- Loki is a log aggregation system designed to work like Prometheus: it indexes only metadata labels rather than full log content, making it highly storage-efficient compared to Elasticsearch

**How it's used here.**

- `logback-spring.xml` configures two appenders: `ASYNC_CONSOLE` (colour-formatted, buffered through a 512-message queue) and `JSON_FILE` (Logstash JSON format, rolling daily, gzip-compressed, 30-day retention, max 100 MB per file)
- The `logstash-logback-encoder` library serialises each log event as a JSON object embedding `traceId` and `spanId` fields so that Loki log entries can be linked directly to Tempo traces in Grafana
- The JSON files under `logs/` are the source Loki (or a log-shipper sidecar) would tail and push into the aggregation backend

---

### Spring WebFlux / Project Reactor (`Flux`)

**What it is.**

- Project Reactor is a reactive-streams library for the JVM; `Flux<T>` represents an asynchronous sequence of 0–N items
- Spring WebFlux uses it for non-blocking HTTP handling, allowing a single thread to serve many concurrent in-flight requests

**How it's used here.**

- The streaming chat endpoint (`/chat/stream`) returns a `Flux<String>` that `ChatBackend` implementations produce
- In `LocalChatBackend`, Spring AI's `ChatClient.stream()` method returns a `Flux<String>` of token chunks directly from the model
- In `GatewayChatBackend`, `WebClient` (Spring's reactive HTTP client) connects to the gateway's SSE stream and returns the same `Flux<String>`
- The controller maps this `Flux` to `MediaType.TEXT_EVENT_STREAM_VALUE` so browsers receive a true Server-Sent Events response
- `WebClient` is also used for all non-streaming gateway calls, where `.block()` converts the reactive result to a blocking call within a configured timeout

---

### Apache Tika / PDF and Markdown Document Readers

**What it is.**

- Apache Tika is a content-analysis toolkit that can extract text and metadata from hundreds of file formats (PDF, DOCX, HTML, etc.)
- Spring AI's document readers wrap Tika, PDFBox, and a Markdown parser into a unified `DocumentReader` API

**How it's used here.**

- `spring-ai-tika-document-reader` is on the classpath so the `FileReadService` and RAG flows can ingest arbitrary uploaded files regardless of format
- `spring-ai-pdf-document-reader` provides a dedicated page/paragraph split mode controlled by `app.loader.pdf.mode`
- The pre-loaded corporate travel policy PDFs (`AtlasCorp-TravelPolicy.pdf`, `AtlasCorp_Events_Holidays.pdf`) are referenced as `ClassPathResource` objects for advisor context
- `spring-ai-markdown-document-reader` handles `.md` files in the same unified pipeline

---

### JDBC Chat Memory (Spring AI)

**What it is.**

- Spring AI's `spring-ai-starter-model-chat-memory-repository-jdbc` module stores conversation messages in a relational table (`spring_ai_chat_memory`) and ships its own DDL
- Configured with `initialize-schema: always` so the table is created automatically without a custom migration

**How it's used here.**

- `JdbcChatMemoryRepository` is auto-configured and injected into `AIConfig.chatMemory()`, which wraps it in a `MessageWindowChatMemory` with a 50-message window
- The `MessageChatMemoryAdvisor` on every `ChatClient` call looks up the last 50 messages for the `conversationId` parameter passed in each request
- Prior messages are prepended to the prompt as context, and the new exchange is appended after the model responds
- This gives the service stateful multi-turn memory across HTTP requests with no in-process state — memory survives restarts automatically

---

### Lombok

**What it is.**

- Lombok is a Java annotation processor that generates boilerplate code (constructors, getters, `toString`, `equals/hashCode`, builders, loggers) at compile time
- Reduces source verbosity without adding runtime overhead — all generated code is plain Java bytecode

**How it's used here.**

- `@RequiredArgsConstructor` on service and component classes generates the constructor used by Spring's constructor injection — no `@Autowired` annotations needed
- `@Slf4j` injects a `log` field backed by SLF4J/Logback into every annotated class
- `@AllArgsConstructor` appears on `AudioService` where all fields need explicit initialisation
- Lombok is excluded from the final fat-jar via `spring-boot-maven-plugin`'s exclude list because it is a compile-time-only tool with no runtime dependency

---

### Testcontainers

**What it is.**

- Testcontainers is a Java library that starts real Docker containers during JUnit tests and cleans them up afterwards
- `@ServiceConnection` (a Spring Boot 3.1+ annotation) wires the container's dynamic port and credentials directly into the application context with no manual property overriding

**How it's used here.**

- `TestcontainersConfiguration` defines a `@ServiceConnection PostgreSQLContainer<>("postgres:18")` bean
- When `LLMApplicationTests` loads the context, Spring Boot auto-overrides the datasource URL with the container's random mapped port
- Flyway migrations, the JDBC chat-memory schema, and all JDBC queries in tests run against a real Postgres 18 instance
- The suite is fully self-contained and runs in CI with only Docker available — no locally provisioned database required

---

### JaCoCo

**What it is.**

- JaCoCo (Java Code Coverage) is a bytecode-instrumentation tool that measures which lines, branches, and instructions are exercised by the test suite
- Produces HTML, XML, and CSV reports consumed by CI pipelines and IDEs

**How it's used here.**

- The `jacoco-maven-plugin` (version 0.8.13) is configured with two executions: `prepare-agent` (attaches the JaCoCo agent before tests run) and `report` (bound to the `verify` phase, producing HTML/XML reports under `target/site/jacoco`)
- Running `./mvnw verify` compiles, runs all tests, and generates the full coverage report in one step

---

### Git Commit ID Maven Plugin

**What it is.**

- The `git-commit-id-maven-plugin` reads Git metadata (branch, commit hash, timestamp, dirty flag) at build time and writes it to `git.properties` on the classpath
- Makes every built artifact self-describing — the running application knows exactly which commit it was built from

**How it's used here.**

- The plugin runs during the `initialize` phase and generates `target/classes/git.properties`
- Spring Boot Actuator's `/ai/actuator/info` endpoint automatically exposes these properties (enabled by `management.info.git.mode: full`)
- Every running instance reports its exact commit, branch, and dirty-flag state — invaluable for verifying deployments and correlating incidents to code changes

---

### Docker Compose

**What it is.**

- Docker Compose is a tool for defining and running multi-container applications from a single YAML file
- Health checks between services enable controlled startup ordering without manual delays

**How it's used here.**

- `docker-compose.yml` defines seven services: `postgres` (port 5432), `redis` (port 6379), `redisinsight` (port 5540), `tempo` (ports 3200/4317/4318), `loki` (port 3100), `prometheus` (port 9090), and `grafana` (port 3000)
- All services have health checks so `docker compose up -d` waits for each service to be genuinely ready before marking it as started
- Named volumes (`postgres_data`, `redis_data`, etc.) provide persistence of data across container restarts
- The application itself does **not** run in Compose — it starts separately on the host via `./mvnw spring-boot:run` and reaches containers on `localhost`
- `spring.docker.compose.enabled=false` prevents Spring Boot's built-in Compose integration from re-managing the already-running containers
