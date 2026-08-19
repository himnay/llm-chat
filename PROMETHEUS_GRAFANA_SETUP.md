# <span style="color:hsl(97,68%,32%)">Observability Setup — Prometheus, Grafana, Tempo, Loki</span>

This service ships a full observability stack mirroring `llm-gateway` / `llm-rag-pipeline`:
**metrics** (Prometheus), **traces** (Tempo), **logs** (Loki), visualised in **Grafana**.

## <span style="color:hsl(148,68%,32%)">1. Start the stack</span>

```bash
docker compose up -d
```

This starts Postgres, Redis, RedisInsight, Prometheus, Grafana, Tempo and Loki.
The chat application itself runs on the **host** (port `8082`, context path `/ai`), so Prometheus
scrapes it at `host.docker.internal:8082/ai/actuator/prometheus` (see `observability/prometheus.yml`).

## <span style="color:hsl(200,68%,44%)">2. Run the app</span>

```bash
export OPENAI_API_KEY=sk-...
export STABILITYAI_API_KEY=sk-...     # only needed for image generation
./mvnw spring-boot:run
```

## <span style="color:hsl(251,68%,44%)">3. Endpoints</span>

| What              | URL                                          |
|-------------------|----------------------------------------------|
| App health        | http://localhost:8082/ai/actuator/health     |
| Prometheus scrape | http://localhost:8082/ai/actuator/prometheus |
| Prometheus UI     | http://localhost:9090                        |
| Grafana           | http://localhost:3000  (admin / admin)       |
| Tempo (traces)    | queried via Grafana                          |
| Loki (logs)       | queried via Grafana                          |

## <span style="color:hsl(303,68%,44%)">4. Grafana</span>

Datasources (Prometheus, Tempo, Loki) and the **LLM Chat** dashboard are auto-provisioned from
`observability/grafana/provisioning/`. Open Grafana → Dashboards → *LLM Chat* folder. The starter
dashboard includes:

- HTTP request rate & p95 latency (`http_server_requests_*`)
- HTTP error rate (4xx/5xx)
- JVM heap usage, process CPU and live threads

## <span style="color:hsl(354,68%,44%)">5. Tracing & log correlation</span>

`management.tracing.sampling.probability=1.0` samples every request and exports spans to Tempo over
OTLP (`http://localhost:4318`). JSON logs (`logs/llm-chat.json`) carry `traceId`/`spanId`, and the
Loki datasource is configured with a derived field so you can jump **log → trace** in Grafana.

## <span style="color:hsl(46,68%,32%)">Tuning</span>

- Reduce trace volume in production by lowering `management.tracing.sampling.probability`.
- Point `OTEL_EXPORTER_OTLP_ENDPOINT` at a remote collector if not using the local Tempo.
