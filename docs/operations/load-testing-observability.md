# Load Testing Observability

This document describes the local Prometheus/Grafana stack used while k6 load tests run from a separate Loader EC2 instance.

## Run

On the Backend EC2, start the app, MySQL, Prometheus, and Grafana together.

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.observability.yml \
  up -d --build
```

Default published ports are bound to localhost to avoid accidental public exposure.

| Service | Default address | Note |
| --- | --- | --- |
| Backend | `127.0.0.1:8080` | Existing compose default |
| Prometheus | `127.0.0.1:9090` | Prefer SSH tunnel |
| Grafana | `127.0.0.1:3000` | Prefer SSH tunnel |

Use an SSH tunnel from your local machine to view Grafana.

```bash
ssh -L 3000:127.0.0.1:3000 ec2-user@<backend-ec2-public-ip>
```

Then open `http://127.0.0.1:3000`.

## Metrics To Watch

- HTTP request rate
- HTTP 5xx error ratio
- HTTP latency p95/p99
- Hikari active/idle/pending connections
- JVM memory
- process CPU

## Rate Limit

If the goal is backend throughput, disable the application rate limit in staging.

```bash
APP_SECURITY_RATE_LIMIT_ENABLED=false
```

If the goal is public abuse-defense validation, keep rate limiting enabled and use a separate scenario.

## Prometheus Endpoint

The backend exports Prometheus metrics here:

```text
/actuator/prometheus
```

Prometheus scrapes `app:8080` on the Compose internal network. This endpoint does not need to be publicly exposed.

