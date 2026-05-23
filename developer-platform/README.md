# Developer Utility Platform

## 1. Project Overview
The Developer Utility Platform is an integrated suite of tools designed to optimize, monitor, and sandbox database and API operations. It provides comprehensive static SQL analysis and deep query explain capabilities backed by PostgreSQL to help developers find execution bottlenecks instantly. The platform features an intelligent, multi-algorithm rate limiting system backed by Redis alongside a dynamic HTTP request sandbox and robust transaction audit logging.

---

## 2. Architecture Diagram

```
React (3000) ──> Dev Toolkit (8080) ──> Rate Limiter (8081) ──> Redis (6379)
                      │
                      └──> PostgreSQL (5432)
```

---

## 3. Quick Start

Run these exactly three commands to clone, navigate to the directory, and bring up the entire multi-container platform in Docker:

```bash
git clone <repository-url>
cd developer-platform
docker-compose up --build
```

---

## 4. Features List

*   **SQL Analyser**: Parses queries statically to score safety/performance, detects performance issues (like full table scans, wildcard joins, and deep nesting), and offers direct optimization recommendations.
*   **Execution Plan**: Automatically runs PostgreSQL `EXPLAIN ANALYZE` commands on arbitrary queries, maps cost breakdowns, and highlights major execution bottlenecks (`cost > 1000`) dynamically.
*   **Endpoint Sandbox**: A full-featured REST client enabling developers to simulate API requests (GET, POST, PUT, DELETE, etc.) with custom headers, record execution latency, and review a table of the last 50 requests.
*   **Rate Monitor**: A real-time rate policy monitor providing visual progress meters for remaining allowances, countdown indicators for policy window resets, and simulated traffic dispatchers.
*   **Audit Logger**: Leverages Aspect-Oriented Programming (Spring AOP) to automatically intercept API controller transactions, logging correlation IDs, rate limit outcomes, and latency profiles to PostgreSQL.

---

## 5. API Reference

### Dev Toolkit Service (Port `8080`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/sql/analyse` | Evaluates static quality check rules, providing safety scoring and table scan warnings. |
| `POST` | `/api/sql/explain` | Invokes PostgreSQL plan explanation, extracts execution node hierarchies, and highlights cost bottlenecks. |
| `POST` | `/api/sandbox/request` | Dispatches arbitrary HTTP REST requests through a sandbox template, computing latency. |
| `GET` | `/api/sandbox/history` | Fetches historical log entries representing the 50 most recent sandbox executions. |
| `GET` | `/api/audit/logs` | Lists persistent audit logs, with support for filtering by dynamic correlation ID parameter. |
| `DELETE` | `/api/audit/logs` | Purges the PostgreSQL transactional log database completely. |

### Rate Limiter Service (Port `8081`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/policies` | Creates or replaces a custom rate limiting rule mapping endpoints to algorithms. |
| `GET` | `/api/policies` | Lists all active rate limiting policies currently residing in Redis. |
| `GET` | `/api/policies/{*endpoint}` | Fetches individual rate policy details associated with a normalized path string. |
| `DELETE` | `/api/policies/{*endpoint}` | Removes a rate limiting configuration from the Redis database. |
| `POST` | `/api/validate` | Manually validates a request for a client ID against matching rate limits. |

---

## 6. Rate Limit Policies

The system automatically provisions these default policies in Redis on startup if they do not already exist:

| Endpoint | Algorithm | Max Requests | Window Seconds |
| :--- | :--- | :---: | :---: |
| `/api/sql/analyse` | `SLIDING_WINDOW` | 20 | 60 |
| `/api/sql/explain` | `TOKEN_BUCKET` | 5 | 60 |
| `/api/sandbox/request` | `SLIDING_WINDOW` | 10 | 60 |
| `/api/audit/logs` | `SLIDING_WINDOW` | 30 | 60 |

---

## 7. How to Test Rate Limiting

The Rate Limiter expects clients to pass identification via the `X-Client-Id` header (or mapped in controllers).

### Step 1: Send Request within Limit
Dispatch a query to the SQL static analyzer under the client ID `developer-123`:
```bash
curl -X POST http://localhost:8080/api/sql/analyse \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: developer-123" \
  -d '{"sql": "SELECT * FROM users WHERE id = 1"}'
```
*Response Header Indicators:*
- `X-RateLimit-Remaining: 19`
- `X-RateLimit-Reset: 59`

### Step 2: Fire Concurrent Requests to Exceed Limits
Submit multiple concurrent requests to the plan explainer to trip the token bucket threshold (max 5 requests per minute):
```bash
for i in {1..6}; do
  curl -i -X POST http://localhost:8080/api/sql/explain \
    -H "Content-Type: application/json" \
    -H "X-Client-Id: developer-123" \
    -d '{"sql": "SELECT * FROM users"}'
done
```
On the 6th execution, the platform will return `HTTP 429 Too Many Requests` alongside rate limit exhaustion details.

---

## 8. Tech Stack

| Technology Layer | Component / Package | Purpose |
| :--- | :--- | :--- |
| **Frontend** | React (v19) | Interactive user dashboards, tab wiring, state evaluation, dynamic request dispatches. |
| **Backend** | Spring Boot (v3.2.5) | Enterprise microservice framework containing MVC mappings, Spring Data, and AOP. |
| **Database** | PostgreSQL (v15) | Relational engine storing request/sandbox logs and persistent transaction audits. |
| **Caching/Limiting** | Redis (v7) | High-speed cache for multi-algorithm rate limit sliding windows and token buckets. |
| **Orchestration** | Docker & Compose | Seamless container definitions, port variables, and service dependency graphs. |
| **APIs / Build** | Maven, Axios, Lombok | Build automation, AJAX query dispatchers, and boilerplates reducers. |
