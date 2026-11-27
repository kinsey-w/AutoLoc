# AutoLoc monitoring

Actuator exposes metrics. Micrometer formats them for Prometheus, which scrapes every 5 seconds. Grafana queries Prometheus and displays the dashboard.

## Start

1. Reload Maven and start `AutolocApiApplication` in IntelliJ on port 8080 with `DB_PASSWORD` set.
2. Start Docker Desktop and run these commands from the project folder:

```powershell
docker --version
docker compose version
docker run --rm hello-world
docker compose up -d
docker compose ps
```

| URL | Check |
| --- | --- |
| http://localhost:8080/actuator/health | Status is UP. |
| http://localhost:8080/actuator/prometheus | Metric names and values appear. |
| http://localhost:9090/targets | `autoloc-api` is UP. |
| http://localhost:3000/d/autoloc-monitoring | Five-panel dashboard. |

Grafana provisions the Prometheus data source and dashboard from `monitoring/grafana`. On a fresh container, sign in with `admin` / `admin` and change the password. Use the dashboard JSON file for permanent panel edits; provisioning can overwrite UI edits.

## Metrics

| Panel | Purpose |
| --- | --- |
| JVM memory used | Heap and non-heap memory in bytes. |
| CPU usage | CPU used by the Spring Boot process. |
| HTTP request rate | Requests per second. |
| HTTP response time p95 | Latency below which 95% of requests fall. |
| Live threads | Current JVM thread count. |

Send repeated requests to `/api/public/ping` and `/api/hello` using the classroom account. Wait for several scrapes, select Last 5 minutes and refresh. Rates need at least two scrapes; p95 needs request samples.

Run `.\scripts\verify-monitoring.ps1` to check health, metric samples, the Prometheus target and Grafana. Use `-SkipContainers` to check only the application.

## Configuration choices

- Prometheus reaches the host app at `host.docker.internal:8080`.
- Grafana reaches Prometheus at `http://prometheus:9090` inside Docker.
- Security issue addressed: anonymous scraping needs access. Only `/actuator/health` and `/actuator/prometheus` are permitted; API role rules stay active.
- Histogram buckets are enabled so the p95 query has data.
- Detailed health is enabled for this local lab. Restrict it before production use.

Stop the containers with `docker compose down`; saved metrics and Grafana settings remain in Docker volumes. Stop Spring Boot separately.

Capture Prometheus Targets showing UP and the populated Grafana dashboard for submission.

## Validation status

Live verification passed on 2026-11-27: AutoLoc and MySQL are UP, all required metrics are exposed, Prometheus scrapes `autoloc-api` every 5 seconds without errors, and Grafana is healthy. All five dashboard queries return data. The verification script passed; submission screenshots still need capturing.
