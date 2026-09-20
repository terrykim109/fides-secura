# Fides Secura

Java/Spring Boot transaction API that processes transfers with concurrency controls and detects account takeover by correlating authentication and transfer events into auditable incidents.

**Status:** auth slice live (register / login / JWT / lockout / security events). Transfers, ATO correlation, and CI are not implemented yet — see [Limitations](#limitations).

## Why this exists

My interest in account takeover security kinda began when my Facebook account was hacked years ago and I could never recover my account. That experience showed me how important account protection is, even more so when a compromised account can move money. Fides Secura is my attempt to build that protection into a transaction API, with three goals:

- Keep money movement correct under concurrency (planned)
- Detect a stolen-login payout pattern and **hold** the transfer (planned)
- Auth that emits real security events and locks after repeated failures (**done**)

## Why this shape

| Choice | Why | Not chosen |
|---|---|---|
| Modular monolith (`api/` + thin `web/`) | Keeps transaction and security logic in one consistency boundary | Kafka / many services before needed |
| PostgreSQL + Flyway | Real constraints and migrations | H2 as the primary database |
| Bearer JWT | Simple for Vite SPA; document XSS trade-off | httpOnly cookies (valid alternative) |
| Temporary lockout after 5 failures | Real control that writes `ACCOUNT_LOCKED` events | Log-only “fake” lockout |

## Stack

| Layer | Choice |
|---|---|
| API | Java 21, Spring Boot 3, Spring Security, JJWT |
| DB | PostgreSQL 16, Flyway |
| Web | React, Vite, TypeScript |
| Ops | Docker Compose (Postgres) |

## Repository layout

```
api/                 Spring Boot API 
web/                 Thin React shell
docker-compose.yml   Postgres
.env.example         Env var template (never commit real secrets)
```

## How to run

### Prerequisites

- JDK 21+ and Maven
- Node 20+ (optional, for the web UI)
- Docker (for Postgres)

### 1. Database

```bash
docker compose up -d
```

Defaults (see `.env.example`): `localhost:5432`, database/user/password `bank` / `bank` / `bank`.

### 2. API

```bash
cd api
mvn spring-boot:run
```

```bash
curl http://localhost:8080/api/v1/health
```

### 3. Web (optional)

```bash
cd web
npm install
npm run dev
```

Open http://localhost:5173 — Vite proxies `/api` to port 8080.

### Tests

```bash
cd api
mvn test
```

## Auth usage

Register (customer/teller only; cannot self-register ADMIN/ANALYST):

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"customer@fidessecura.bank\",\"password\":\"password1\",\"fullName\":\"Demo Customer\",\"role\":\"CUSTOMER\"}"
```

Login:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"customer@fidessecura.bank\",\"password\":\"password1\"}"
```

Authenticated profile (use `accessToken` from login/register):

```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

Five consecutive failed logins lock the account for 15 minutes and write `LOGIN_FAILURE` / `ACCOUNT_LOCKED` rows to `security_events`.

## What works today

| Method | Path | Auth | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/health` | public | Liveness JSON |
| `GET` | `/api/v1/auth/status` | public | Auth feature flags |
| `POST` | `/api/v1/auth/register` | public | Create user + JWT |
| `POST` | `/api/v1/auth/login` | public | JWT; lockout + events |
| `GET` | `/api/v1/auth/me` | Bearer JWT | Current user |
| `GET` | `/api/v1/security/capabilities` | public | Sample risk helper output only |

## Planned workflow (transfers + ATO not built yet)

```
failed logins → success from new IP → large transfer
        → PENDING_REVIEW + linked incident + tests
```

## Limitations

- No transfers, incidents, or ATO correlator yet
- `/security/capabilities` sample scoring is not wired to live transfers
- In-memory per-IP login rate limit (resets on restart; not distributed)
- No GitHub Actions CI yet

