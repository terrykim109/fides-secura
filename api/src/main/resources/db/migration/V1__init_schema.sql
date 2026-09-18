-- Fides Secura foundation schema.
-- Banking tables are the protected asset; security_events + incidents power SOC-lite detection.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    role            VARCHAR(32)  NOT NULL
                    CHECK (role IN ('CUSTOMER', 'TELLER', 'ANALYST', 'ADMIN')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_logins   INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT         NOT NULL REFERENCES users (id),
    account_number  VARCHAR(34)    NOT NULL UNIQUE,
    account_type    VARCHAR(32)    NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    currency        CHAR(3)        NOT NULL DEFAULT 'CAD',
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    version         BIGINT         NOT NULL DEFAULT 0,
    status          VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_customer ON accounts (customer_id);

CREATE TABLE transfers (
    id                  BIGSERIAL PRIMARY KEY,
    idempotency_key     VARCHAR(100)   NOT NULL,
    from_account_id     BIGINT         NOT NULL REFERENCES accounts (id),
    to_account_id       BIGINT         NOT NULL REFERENCES accounts (id),
    amount              NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency            CHAR(3)        NOT NULL DEFAULT 'CAD',
    status              VARCHAR(32)    NOT NULL
                        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'FLAGGED')),
    initiated_by        BIGINT         NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transfers_idempotency UNIQUE (initiated_by, idempotency_key),
    CONSTRAINT chk_transfers_different_accounts CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_transfers_from ON transfers (from_account_id, created_at DESC);
CREATE INDEX idx_transfers_to ON transfers (to_account_id, created_at DESC);

-- Business / compliance trail (who changed money or config).
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(64),
    entity_id       VARCHAR(64),
    ip_address      VARCHAR(64),
    details         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor_time ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_action_time ON audit_logs (action, created_at DESC);

-- Security telemetry for detection (auth failures, lockouts, rate limits, anomalies).
CREATE TABLE security_events (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(64)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL DEFAULT 'INFO'
                    CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    actor_user_id   BIGINT       REFERENCES users (id),
    subject_user_id BIGINT       REFERENCES users (id),
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    correlation_id  VARCHAR(64),
    payload         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_events_type_time ON security_events (event_type, created_at DESC);
CREATE INDEX idx_security_events_subject_time ON security_events (subject_user_id, created_at DESC);
CREATE INDEX idx_security_events_ip_time ON security_events (ip_address, created_at DESC);

CREATE TABLE fraud_alerts (
    id              BIGSERIAL PRIMARY KEY,
    transfer_id     BIGINT         NOT NULL REFERENCES transfers (id),
    risk_score      INT            NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    reasons         TEXT           NOT NULL,
    status          VARCHAR(32)    NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED')),
    reviewed_by     BIGINT         REFERENCES users (id),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ
);

CREATE INDEX idx_fraud_status ON fraud_alerts (status, created_at DESC);

-- Correlated detections (SIEM-lite output), e.g. brute-force → login → large transfer.
CREATE TABLE incidents (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    severity        VARCHAR(16)  NOT NULL
                    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status          VARCHAR(32)  NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN', 'INVESTIGATING', 'CONTAINED', 'CLOSED')),
    rule_name       VARCHAR(100) NOT NULL,
    summary         TEXT         NOT NULL,
    subject_user_id BIGINT       REFERENCES users (id),
    transfer_id     BIGINT       REFERENCES transfers (id),
    assigned_to     BIGINT       REFERENCES users (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_status_sev ON incidents (status, severity, created_at DESC);

CREATE TABLE incident_events (
    incident_id     BIGINT NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    security_event_id BIGINT NOT NULL REFERENCES security_events (id),
    PRIMARY KEY (incident_id, security_event_id)
);
