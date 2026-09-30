# Fides Secura

Java/Spring Boot transaction API that processes transfers with concurrency controls and detects account takeover by correlating authentication and transfer events into auditable incidents.

**Status:** auth, accounts/transfers, and ATO correlation are live. Held transfers create incidents analysts can approve/reject. CI is not implemented yet — see [Limitations](#limitations).

## Why this exists

My interest in account takeover security kinda began when my Facebook account was hacked years ago and I could never recover my account. That experience showed me how important account protection is, even more so when a compromised account can move money. Fides Secura is my attempt to build that protection into a transaction API, with three goals:

- Keep money movement correct under concurrency (**done** — ordered `SELECT … FOR UPDATE` + balance checks)
- Detect a stolen-login payout pattern and **hold** the transfer (**done** — `PENDING_REVIEW` + incident)
- Auth that emits real security events and locks after repeated failures (**done**)

## Why this shape

| Choice | Why | Not chosen |
|---|---|---|
| Modular monolith (`api/` + thin `web/`) | Keeps transaction and security logic in one consistency boundary | Kafka / many services before needed |
| PostgreSQL + Flyway | Real constraints and migrations | H2 as the primary database |
| Bearer JWT | Simple for Vite SPA; document XSS trade-off | httpOnly cookies (valid alternative) |
| Temporary lockout after 5 failures | Real control that writes `ACCOUNT_LOCKED` events | Log-only “fake” lockout |
| `Idempotency-Key` + unique `(initiated_by, key)` | Safe client retries without double-pay | “Hope the network is fine” |
| Pessimistic row locks (id-ordered) | Avoid lost updates and deadlocks on crossed transfers | Balance update without locking |
| Hold without moving balances | Analyst reject leaves money untouched | Auto-complete then claw back |

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

## Accounts & transfers

Open an account, deposit, then transfer with an `Idempotency-Key` header (see prior README examples). Successful clear transfers write `TRANSFER_COMPLETED`.

## ATO detection (demo path)

Rule `BRUTE_FORCE_THEN_LARGE_TRANSFER` (defaults: 3+ failures in 30 minutes, login success from a **new** IP, transfer ≥ **5000 CAD** from that IP):

1. Fail login ≥3 times (same user).
2. Succeed login from a different IP than any prior `LOGIN_SUCCESS` for that user (send `X-Forwarded-For` in curl).
3. Attempt a ≥5000 CAD transfer from that session IP.

Result: transfer status `PENDING_REVIEW` (balances **not** moved), `TRANSFER_FLAGGED` event, and an `OPEN` incident linking the contributing auth events.

Promote an analyst (SQL, after register a customer you trust for the role):

```sql
UPDATE users SET role = 'ANALYST' WHERE email = 'analyst@fidessecura.bank';
```

Then:

```bash
curl -s http://localhost:8080/api/v1/incidents \
  -H "Authorization: Bearer <analystToken>"

curl -s -X POST http://localhost:8080/api/v1/incidents/1/approve \
  -H "Authorization: Bearer <analystToken>"
```

`approve` posts the held transfer (moves balances → `COMPLETED`). `reject` marks transfer `FAILED` and contains the incident (balances unchanged).

## What works today

| Method | Path | Auth | Behavior |
|---|---|---|---|
| `GET` | `/api/v1/health` | public | Liveness JSON |
| `GET` | `/api/v1/auth/status` | public | Auth feature flags |
| `POST` | `/api/v1/auth/register` | public | Create user + JWT |
| `POST` | `/api/v1/auth/login` | public | JWT; lockout + events |
| `GET` | `/api/v1/auth/me` | Bearer JWT | Current user |
| `POST` | `/api/v1/accounts` | Bearer JWT | Open CHECKING/SAVINGS |
| `GET` | `/api/v1/accounts` | Bearer JWT | List my accounts |
| `GET` | `/api/v1/accounts/{id}` | Bearer JWT | Get my account |
| `POST` | `/api/v1/accounts/{id}/deposit` | Bearer JWT | Credit (demo funding) |
| `POST` | `/api/v1/transfers` | Bearer JWT | Idempotent transfer; may hold |
| `GET` | `/api/v1/transfers` | Bearer JWT | My visible transfers |
| `GET` | `/api/v1/incidents` | ANALYST/ADMIN | Open (or `?status=all`) incidents |
| `POST` | `/api/v1/incidents/{id}/approve` | ANALYST/ADMIN | Release held transfer |
| `POST` | `/api/v1/incidents/{id}/reject` | ANALYST/ADMIN | Drop held transfer |
| `GET` | `/api/v1/security/capabilities` | public | Sample + status probe |

## Limitations

- No GitHub Actions CI / Testcontainers e2e harness yet
- Analyst role must be granted via SQL (no self-register)
- Deposit is a demo convenience, not a full payments rail
- In-memory per-IP login rate limit (resets on restart; not distributed)
- ATO rule is intentionally narrow (one correlation pattern)
