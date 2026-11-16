# Fides Secura

I built this because my Facebook got taken over years ago and I never got it back. That stuck with me — not as “cyber buzzwords,” but as the feeling that once someone else owns the login, you’re done. Banking makes that worse: a stolen session doesn’t just post as you, it can move money.

So this is a small Java/Spring Boot transaction API where the interesting part isn’t the CRUD. It’s:

1. Can transfers stay correct when two requests hit at once?
2. Can the system notice a brute-force → new-IP login → large payout pattern and **hold** the money before it leaves?
3. Can an analyst see *why* it was held and approve or reject it?

**What works today:** register/login with lockout + security events, accounts & idempotent transfers with row locking, ATO hold → incident → analyst decision, seeded demo users, Testcontainers e2e, GitHub Actions CI.

I’m aiming this at bank / backend / AppSec-adjacent internships (Toronto-area). It’s deliberately narrow on purpose — one vertical slice done honestly beats a fake “fraud platform.”

## Why these choices (not buzzword bingo)

| I chose… | Because… | I didn’t… |
|---|---|---|
| One Spring Boot app + thin React | Money + detection need the same transaction boundary | Microservices / Kafka before I had a working transfer |
| Postgres + Flyway | Constraints and migrations are the real thing banks care about | `ddl-auto=update` and hope |
| Bearer JWT for the SPA | Fast to demo; I’m honest about XSS | Pretend httpOnly cookies while I still have a Vite toy UI |
| Idempotency-Key on transfers | Retries shouldn’t double-pay | “Just don’t click twice” |
| Lock accounts in id order | Deadlocks and lost updates are the interview question | Update balances and pray |
| Hold **before** moving balances | If the analyst rejects, nothing left the account — the outcome I wish recovery systems had | Auto-send then claw back |

## Architecture

One process owns auth, money, and detection. React is a shell so you can click through the story; the security decisions are in the API.

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

### Request path

```text
HTTP → JwtAuthenticationFilter → Controller → @Transactional Service
    → Repository / row locks → Postgres (+ security_events / incidents)
```

### The ATO path I care about

```text
Failed logins → success from a new IP → large transfer attempt
                    ↓
              correlator hits
                    ↓
         PENDING_REVIEW (balances untouched)
                    ↓
              OPEN incident
                    ↓
         analyst approve → money moves
         analyst reject  → FAILED, still untouched
```

That’s the story: don’t let the payout complete while you’re still figuring out that the login was stolen.

## Stack

Java 21 · Spring Boot 3 · Spring Security · JJWT · PostgreSQL 16 · Flyway · Testcontainers · React · Vite · TypeScript · Docker Compose · GitHub Actions

## Getting started

Needs JDK 21+, Maven, Docker. Node 20+ if you want the UI.

```bash
docker compose up -d
cd api && mvn spring-boot:run
```

API: `http://localhost:8080`

```bash
cd web && npm install && npm run dev
```

UI: `http://localhost:5173` (Vite proxies `/api`).

```bash
cd api && mvn test
```

## Demo accounts (seeded so you don’t need SQL)

| Email | Password | Role | Notes |
|---|---|---|---|
| `customer@fidessecura.bank` | `password1` | CUSTOMER | ~$10k CAD checking; prior “home” login IP `192.168.1.10` |
| `merchant@fidessecura.bank` | `password1` | CUSTOMER | Transfer destination |
| `analyst@fidessecura.bank` | `password1` | ANALYST | Approve / reject holds |

Turn off with `DEMO_SEED=false`.

## Walk the ATO demo (this is the point of the repo)

1. Sign in as **customer** (login page has a “Fill customer” chip).
2. Fail login **3×** with a wrong password. For a clean “new IP” story via curl, send `X-Forwarded-For: 203.0.113.50`.
3. Sign in successfully from that new IP.
4. Transfer **≥ 5000 CAD** to the merchant account.
5. Status should be `PENDING_REVIEW` — balances **don’t** change.
6. Sign in as **analyst** → Security console → Approve or Reject.

If step 5 completes the transfer instead of holding it, the correlator didn’t see enough failures / new-IP success in the lookback window. That’s a feature of a narrow rule, not magic AI fraud.

## The rule (one pattern, on purpose)

`BRUTE_FORCE_THEN_LARGE_TRANSFER`

Defaults: **3+** failures in **30 minutes**, then `LOGIN_SUCCESS` from an IP that isn’t in the user’s prior success history, then a transfer **≥ 5000 CAD** from that IP → hold + `TRANSFER_FLAGGED` + open incident.

I kept it to one correlator so I can explain every branch in an interview. Broadening it without tests would just be theater.

## Deploy note (Vercel)

Vercel only hosts the React shell. Host the API + Postgres somewhere else, set `VITE_API_BASE` for the build and `CORS_ALLOWED_ORIGINS` on the API. The login page alone won’t prove detection — the hold + analyst screen will.

## CI

`.github/workflows/ci.yml` runs API tests (ATO e2e via Testcontainers when Docker is available) and builds `web/`.

## What this is not (yet / on purpose)

- Not a full bank core, not PCI, not “production fraud AI”
- Deposit is a demo faucet so you can fund accounts quickly
- Login rate limit is in-memory per IP (fine for a laptop demo, not a cluster)
- One ATO pattern — I’d rather defend one rule than list twenty I never wired up

If you’re reading this as a recruiter: ask me to walk the race on transfers, or the hold path end-to-end. That’s where the project lives.
