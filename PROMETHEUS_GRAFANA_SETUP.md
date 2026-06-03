# Observability Setup — Prometheus, Grafana, Tempo, Loki

This service ships a full observability stack mirroring `llm-gateway` / `llm-rag-pipeline`:
**metrics** (Prometheus), **traces** (Tempo), **logs** (Loki), visualised in **Grafana**.

## 1. Start the stack

```bash
docker compose up -d
```

This starts Postgres, Redis, RedisInsight, Prometheus, Grafana, Tempo and Loki.
The chat application itself runs on the **host** (port `8082`, context path `/ai`), so Prometheus
scrapes it at `host.docker.internal:8082/ai/actuator/prometheus` (see `observability/prometheus.yml`).

## 2. Run the app

```bash
export OPENAI_API_KEY=sk-...
export STABILITYAI_API_KEY=sk-...     # only needed for image generation
./mvnw spring-boot:run
```

## 3. Endpoints

| What              | URL                                                  |
|-------------------|------------------------------------------------------|
| App health        | http://localhost:8082/ai/actuator/health             |
| Prometheus scrape | http://localhost:8082/ai/actuator/prometheus         |
| Prometheus UI     | http://localhost:9090                                |
| Grafana           | http://localhost:3000  (admin / admin)               |
| Tempo (traces)    | queried via Grafana                                  |
| Loki (logs)       | queried via Grafana                                  |

## 4. Grafana

Datasources (Prometheus, Tempo, Loki) and the **LLM Chat** dashboard are auto-provisioned from
`observability/grafana/provisioning/`. Open Grafana → Dashboards → *LLM Chat* folder. The starter
dashboard includes:

- HTTP request rate & p95 latency (`http_server_requests_*`)
- HTTP error rate (4xx/5xx)
- JVM heap usage, process CPU and live threads

## 5. Tracing & log correlation

`management.tracing.sampling.probability=1.0` samples every request and exports spans to Tempo over
OTLP (`http://localhost:4318`). JSON logs (`logs/llm-chat.json`) carry `traceId`/`spanId`, and the
Loki datasource is configured with a derived field so you can jump **log → trace** in Grafana.

## Tuning

- Reduce trace volume in production by lowering `management.tracing.sampling.probability`.
- Point `OTEL_EXPORTER_OTLP_ENDPOINT` at a remote collector if not using the local Tempo.
