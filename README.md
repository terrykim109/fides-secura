# Fides Secura

Java/Spring Boot transaction API that processes transfers with concurrency controls and detects account takeover by correlating authentication and transfer events into auditable incidents.

**Status:** Auth, accounts/transfers, and ATO correlation are live. Held transfers create incidents that analysts can approve or reject. CI is not implemented yet.

## Overview

Fides Secura was inspired by a personal account-takeover experience and focuses on what happens when a compromised account can move money.

The project has three core goals:

- **Transaction correctness:** prevent lost updates and concurrency issues during money movement
- **ATO detection:** identify a stolen-login payout pattern and hold the transfer
- **Security telemetry:** emit authentication events and lock accounts after repeated failures

## Architecture

Fides Secura is a modular monolith: one Spring Boot application owns authentication, money movement, ATO detection, and analyst review. React is a thin client; security decisions remain in the API.

```text
┌─────────────┐     JWT / REST      ┌──────────────────────────────┐
│  React/Vite │ ──────────────────► │ Spring Boot API              │
│    :5173    │ ◄────────────────── │ Controllers → Services →     │
└─────────────┘        JSON          │ Repositories → PostgreSQL    │
                                     └──────────────┬───────────────┘
                                                    │
                                                    ▼
                                         ┌────────────────────┐
                                         │   PostgreSQL :5432 │
                                         │   Flyway migrations│
                                         └────────────────────┘
```

### Authenticated Request Path

```text
HTTP request
    ↓
JwtAuthenticationFilter
    ↓
Controller
    ↓
@Transactional Service
    ↓
Repository / row locks
    ↓
PostgreSQL
```

### ATO Detection Flow

```text
Login failures + successful login from new IP
                    ↓
              Transfer request
                    ↓
            AtoDetectionService
                    ↓
          SecurityEventCorrelator
              ┌─────┴─────┐
              ↓           ↓
             hit         miss
              ↓           ↓
       PENDING_REVIEW   COMPLETED
       no balance      debit / credit
       change
              ↓
        OPEN incident
              ↓
      Analyst approve/reject
          ↓           ↓
     COMPLETED      FAILED
```

Transfers that trigger the rule are held **before balances change**.

## Key Design Decisions

| Choice | Reason |
|---|---|
| **Modular monolith** | Keeps transaction and security logic in one consistency boundary |
| **PostgreSQL + Flyway** | Real constraints, transactions, and versioned schema changes |
| **Bearer JWT** | Simple SPA authentication; XSS risk is an intentional trade-off |
| **Idempotency-Key** | Safe client retries without duplicate transfers |
| **Pessimistic row locks, ordered by ID** | Prevent lost updates and reduce deadlock risk |
| **Hold before balance movement** | Rejected transfers leave balances untouched |
| **Security events** | Authentication and transfer activity become auditable signals |

## Stack

**Java 21 · Spring Boot 3 · Spring Security · JJWT · PostgreSQL 16 · Flyway · React · Vite · TypeScript · Docker Compose**

## Account Takeover Detection

The current rule is:

`BRUTE_FORCE_THEN_LARGE_TRANSFER`

Default conditions:

- **3+** failed logins within **30 minutes**
- Successful login from a **new IP**
- Transfer of **5000 CAD or more** from that IP

When triggered:

```text
Transfer          → PENDING_REVIEW
Balance change    → none
Security event    → TRANSFER_FLAGGED
Incident          → OPEN
```

An analyst can then approve the transfer, which moves the balances and completes it, or reject it, which marks it failed without changing balances.

## Getting Started

Requires JDK 21+, Maven, Docker, and Node 20+ for the optional web UI.

```bash
docker compose up -d

cd api
mvn spring-boot:run
```

API: `http://localhost:8080`

Optional UI:

```bash
cd web
npm install
npm run dev
```

Run tests:

```bash
cd api
mvn test
```

## Limitations

- No GitHub Actions CI or Testcontainers end-to-end harness yet
- Analyst role must be granted through SQL
- Deposit is a demo convenience, not a payments rail
- Login rate limiting is in-memory and per-IP, so it is not distributed
- ATO detection currently implements one intentionally narrow correlation pattern