# API Monitoring Dashboard

A small full-stack application that periodically checks a set of HTTP endpoints and
reports their status, latency, and uptime on a live dashboard.

This is **v0.2** of the project: a from-scratch architectural refactor of the original
prototype. Functionality is unchanged from the user's point of view - register a URL,
watch it get checked every 60 seconds, see uptime/latency on the dashboard - but the
internals were rebuilt to be a foundation the project can actually grow on (auth,
alerts, per-service config, deployment, etc.), not a rewrite every time a new feature
is added.

## Architecture

```
┌─────────────────┐        HTTP        ┌──────────────────┐      JDBC      ┌────────────┐
│  React (Vite)    │ ─────────────────▶ │  Spring Boot API  │ ──────────────▶ │ PostgreSQL │
│  Dashboard SPA   │ ◀───────────────── │  (MVC + WebClient)│ ◀────────────── │            │
└─────────────────┘   JSON (ApiResponse) └──────────────────┘                └────────────┘
                                                  │
                                                  │ @Scheduled, every N seconds
                                                  ▼
                                     ┌───────────────────────────┐
                                     │ HealthCheckService         │
                                     │ concurrent, non-blocking   │
                                     │ WebClient calls with retry │
                                     │ + exponential backoff      │
                                     └───────────────────────────┘
                                                  │
                                                  ▼
                                     any HTTP endpoint the user registers
```

**Why WebClient instead of RestTemplate for the health checks:** RestTemplate is
blocking and has been in maintenance mode since Spring 5. With N monitored services,
a blocking sequential loop means one slow or hanging endpoint delays the check of
every other endpoint behind it. WebClient lets the scheduler fire all checks
concurrently (bounded by `monitor.health-check.concurrency`) using Reactor's
non-blocking I/O. The rest of the app (controllers, JPA) is still plain, synchronous
Spring MVC - only the outbound health-check calls are reactive. See
`HealthCheckService` for the full reasoning in code comments, including how blocking
database writes are safely mixed into that reactive pipeline (`Schedulers.boundedElastic()`).

## Folder structure

```
api-monitoring-dashboard/
├── api-monitor-backend/
│   ├── src/main/java/com/monitor/
│   │   ├── config/         # WebClient, CORS, and typed @ConfigurationProperties
│   │   ├── controller/     # Thin HTTP layer only - no business logic
│   │   ├── dto/             # request/ and response/ - never expose entities directly
│   │   ├── exception/      # Global exception handler + custom exceptions
│   │   ├── mapper/         # Entity <-> DTO conversion
│   │   ├── model/           # JPA entities
│   │   ├── repository/     # Spring Data repositories
│   │   ├── scheduler/      # Fixed-rate trigger, delegates all real work to service/
│   │   └── service/         # Business logic (this is where most of the real code lives)
│   ├── src/main/resources/application.yml
│   └── Dockerfile
├── api-monitor-frontend/
│   └── src/
│       ├── api/            # axios client + thin endpoint wrappers
│       ├── components/     # Small, reusable presentational components
│       ├── config/          # Env var access, in one place
│       ├── hooks/           # useServices - all data-fetching/polling logic
│       ├── pages/           # Dashboard (routing is wired up for future pages)
│       └── styles/
├── docker-compose.yml
└── .env.example
```

## Database design

```
api_services                    api_logs
┌────────────────┐              ┌───────────────────────┐
│ id (PK)          │◀────────┐   │ id (PK)                │
│ name              │        └──│ api_service_id (FK)    │
│ url                │            │ status_code            │
│ created_at         │            │ response_time_ms       │
└────────────────┘            │ success                │
                                 │ checked_at              │
                                 └───────────────────────┘
                                 index: (api_service_id, checked_at)
```

Two changes from the original schema, both fixing real correctness/scale issues:

- `api_logs.api_service_id` is now a genuine foreign key (`@ManyToOne`) instead of a
  bare `Long`, so the database enforces referential integrity and deleting a service
  cascades its logs.
- `success` is computed and stored once at write time instead of every reader
  re-deriving "is this a success?" from `status_code == 200` (which was also wrong for
  any 2xx that isn't exactly 200).

**Why PostgreSQL over MySQL:** the project isn't using anything MySQL-specific, and
Postgres has a few concrete advantages for where this is headed - first-class support
on essentially every free-tier PaaS this project is likely to deploy to (Railway,
Render, Neon, Supabase), stronger JSON/array column types that later features
(tags, alert-preference blobs) will likely want, and it avoids any ambiguity around
MySQL's Oracle stewardship. Migration touched the JDBC driver/dialect (Hibernate
auto-detects the dialect from the driver), `application.yml`, and `docker-compose.yml`.

## How monitoring works

1. `HealthCheckScheduler` fires every `monitor.scheduler.fixed-rate-ms` (default 60s).
2. `HealthCheckService` loads every registered service and checks them **concurrently**
   (bounded by `monitor.health-check.concurrency`), not sequentially.
3. Each check calls the target URL via WebClient with a response timeout
   (`monitor.health-check.request-timeout-ms`). Network-level failures (timeout, DNS,
   connection refused) are retried with exponential backoff
   (`monitor.health-check.max-retries` attempts) before being recorded as a failure.
   A real HTTP error response (4xx/5xx) is recorded immediately - it's a real answer
   from the server, not a transient failure worth retrying.
4. Each result is written as one `ApiLog` row.
5. `GET /api/services/metrics/{id}` aggregates all logs for a service in the database
   (`COUNT`/`AVG`/`SUM`, not pulled into the JVM) into uptime %, average latency, and a
   HEALTHY / SLOW / DOWN / UNKNOWN classification, using thresholds from
   `monitor.status.*`.
6. `GET /api/services/logs/{id}` returns the most recent logs (bounded by
   `monitor.logs.default-limit` / `max-limit`), oldest-first, for the latency chart -
   replacing the original endpoint's "return every log this service has ever produced,"
   unbounded query.

## API

Every response is wrapped consistently:

```jsonc
// success
{ "success": true, "data": { ... }, "timestamp": "..." }
// failure
{ "success": false, "message": "...", "data": null, "timestamp": "..." }
```

| Method | Path | Description |
|---|---|---|
| GET | `/api/services` | List all monitored services |
| POST | `/api/services` | Register a new service (`{ "name", "url" }`, validated) |
| GET | `/api/services/logs/{id}?limit=` | Recent logs for a service |
| GET | `/api/services/metrics/{id}` | Uptime, average latency, status for a service |
| GET | `/api/services/summary` | Total number of registered services |

## Running locally (without Docker)

Requires Java 17+, Node 20+, and a local PostgreSQL instance.

```bash
# 1. Start Postgres and create the database
createdb api_monitor

# 2. Backend
cd api-monitor-backend
export DB_USERNAME=postgres DB_PASSWORD=yourpassword
mvn spring-boot:run

# 3. Frontend (separate terminal)
cd api-monitor-frontend
cp .env.example .env
npm install
npm run dev
```

The frontend dev server runs at `http://localhost:5173` and talks directly to the
backend at `http://localhost:8080` (CORS is configured for this by default).

## Running with Docker

```bash
cp .env.example .env   # adjust if you want non-default credentials/ports
docker compose up --build
```

That's it - `docker compose up` builds and starts all three services:

- **postgres** - Postgres 16, data persisted in a named volume (`postgres-data`), gated
  by a `pg_isready` healthcheck.
- **backend** - waits for Postgres to be healthy before starting (avoids the classic
  crash-loop-on-cold-start problem), exposes `8080`.
- **frontend** - Nginx serving the production Vite build, waits for the backend
  healthcheck, exposes `5173` (mapped to Nginx's port 80) and reverse-proxies `/api/*`
  to the backend container over the internal Docker network - so the browser never
  needs to know the backend's address.

Open `http://localhost:5173`.

## Environment variables

See `.env.example` (Docker) and each service's `.env.example` for the full list.
The important ones:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | localhost / postgres / postgres | Database connection |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `MONITOR_SCHEDULER_FIXED_RATE_MS` | `60000` | How often the global check sweep runs |
| `MONITOR_HEALTHCHECK_TIMEOUT_MS` | `5000` | Per-request timeout |
| `MONITOR_HEALTHCHECK_CONCURRENCY` | `8` | Max concurrent checks per sweep |
| `MONITOR_STATUS_SLOW_MS` / `MONITOR_STATUS_DOWN_MS` | `400` / `800` | Latency thresholds for SLOW/DOWN |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` (dev) / `/api` (Docker) | Where the frontend sends requests |

## Future roadmap

Deliberately **not** implemented in this phase - the architecture above was shaped to
make each of these a contained addition rather than a rewrite:

- JWT authentication + multi-user support (services would gain an `owner_id`; every
  repository method and controller endpoint would scope by the authenticated user)
- Per-service configurable interval/timeout/retry policy (the global `monitor.*`
  properties become per-`ApiService` columns; `HealthCheckService` already takes a
  `MonitorProperties` per call, so this is a signature change, not new architecture)
- Email alerts + alert preferences
- Incident timeline, maintenance windows, public status pages
- Flapping detection (services that toggle UP/DOWN repeatedly in a short window)
- Deployment (frontend → Vercel/Netlify, backend → Railway/Render, DB → managed Postgres)
- CI/CD via GitHub Actions
- springdoc-openapi (Swagger UI)
- Spring Boot Actuator + Prometheus/Grafana (the app monitoring itself)
