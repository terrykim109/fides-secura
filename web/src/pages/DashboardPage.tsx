import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  clearAccessToken,
  createTransfer,
  deposit,
  fetchAccounts,
  fetchHealth,
  fetchMe,
  fetchTransfers,
  getAccessToken,
  openAccount,
  type AccountResponse,
  type HealthResponse,
  type TransferResponse,
  type UserResponse,
} from "../api/client";

function money(value: string | number): string {
  const n = typeof value === "number" ? value : Number(value);
  return Number.isFinite(n) ? n.toFixed(2) : String(value);
}

export function DashboardPage() {
  const navigate = useNavigate();
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [transfers, setTransfers] = useState<TransferResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [accountType, setAccountType] = useState<"CHECKING" | "SAVINGS">("CHECKING");
  const [initialDeposit, setInitialDeposit] = useState("100.00");
  const [depositAccountId, setDepositAccountId] = useState("");
  const [depositAmount, setDepositAmount] = useState("50.00");
  const [fromAccountId, setFromAccountId] = useState("");
  const [toAccountId, setToAccountId] = useState("");
  const [transferAmount, setTransferAmount] = useState("25.00");

  const refreshMoney = useCallback(async () => {
    const [accountList, transferList] = await Promise.all([fetchAccounts(), fetchTransfers()]);
    setAccounts(accountList);
    setTransfers(transferList);
    if (accountList.length > 0) {
      setDepositAccountId((prev) => prev || String(accountList[0].id));
      setFromAccountId((prev) => prev || String(accountList[0].id));
      if (accountList.length > 1) {
        setToAccountId((prev) => prev || String(accountList[1].id));
      }
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    if (!getAccessToken()) {
      navigate("/login");
      return;
    }
    Promise.all([fetchHealth(), fetchMe(), fetchAccounts(), fetchTransfers()])
      .then(([healthData, me, accountList, transferList]) => {
        if (cancelled) {
          return;
        }
        setHealth(healthData);
        setUser(me);
        setAccounts(accountList);
        setTransfers(transferList);
        if (accountList.length > 0) {
          setDepositAccountId(String(accountList[0].id));
          setFromAccountId(String(accountList[0].id));
          if (accountList.length > 1) {
            setToAccountId(String(accountList[1].id));
          }
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [navigate]);

  function signOut() {
    clearAccessToken();
    navigate("/login");
  }

  async function onOpenAccount(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await openAccount(accountType, Number(initialDeposit));
      await refreshMoney();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Open account failed");
    } finally {
      setBusy(false);
    }
  }

  async function onDeposit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await deposit(Number(depositAccountId), Number(depositAmount));
      await refreshMoney();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Deposit failed");
    } finally {
      setBusy(false);
    }
  }

  async function onTransfer(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const key = crypto.randomUUID();
      await createTransfer(
        Number(fromAccountId),
        Number(toAccountId),
        Number(transferAmount),
        key,
      );
      await refreshMoney();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Transfer failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <p className="brand">Fides Secura</p>
          <p className="context">
            {user ? `${user.fullName} · ${user.role}` : "Customer shell"}
          </p>
        </div>
        <nav className="top-nav">
          <Link className="text-link" to="/security">
            Security console
          </Link>
          <button type="button" className="text-link linkish" onClick={signOut}>
            Sign out
          </button>
        </nav>
      </header>

      <main className="content">
        <h1>Workspace</h1>
        <p className="lede">
          Open accounts, fund them, and move money with idempotent transfers.
        </p>

        {error && <p className="error">{error}</p>}

        <section className="status-block" aria-live="polite">
          <h2>Session</h2>
          {user && (
            <dl className="kv">
              <div>
                <dt>Email</dt>
                <dd>{user.email}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>{user.role}</dd>
              </div>
            </dl>
          )}
          {health && (
            <dl className="kv">
              <div>
                <dt>API</dt>
                <dd>
                  {health.status} · {health.service}
                </dd>
              </div>
            </dl>
          )}
        </section>

        <section className="status-block">
          <h2>Accounts</h2>
          {accounts.length === 0 ? (
            <p className="muted">No accounts yet. Open one below.</p>
          ) : (
            <ul className="plain-list">
              {accounts.map((a) => (
                <li key={a.id}>
                  <strong>#{a.id}</strong> {a.accountType} · {a.accountNumber} ·{" "}
                  {a.currency} {money(a.balance)} · {a.status}
                </li>
              ))}
            </ul>
          )}

          <form className="inline-form" onSubmit={onOpenAccount}>
            <label>
              Type
              <select
                value={accountType}
                onChange={(e) => setAccountType(e.target.value as "CHECKING" | "SAVINGS")}
              >
                <option value="CHECKING">CHECKING</option>
                <option value="SAVINGS">SAVINGS</option>
              </select>
            </label>
            <label>
              Initial deposit
              <input
                value={initialDeposit}
                onChange={(e) => setInitialDeposit(e.target.value)}
                inputMode="decimal"
              />
            </label>
            <button type="submit" disabled={busy}>
              Open account
            </button>
          </form>

          <form className="inline-form" onSubmit={onDeposit}>
            <label>
              Account id
              <input
                value={depositAccountId}
                onChange={(e) => setDepositAccountId(e.target.value)}
                inputMode="numeric"
              />
            </label>
            <label>
              Amount
              <input
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                inputMode="decimal"
              />
            </label>
            <button type="submit" disabled={busy || accounts.length === 0}>
              Deposit
            </button>
          </form>
        </section>

        <section className="status-block">
          <h2>Transfer</h2>
          <form className="inline-form" onSubmit={onTransfer}>
            <label>
              From
              <input
                value={fromAccountId}
                onChange={(e) => setFromAccountId(e.target.value)}
                inputMode="numeric"
              />
            </label>
            <label>
              To
              <input
                value={toAccountId}
                onChange={(e) => setToAccountId(e.target.value)}
                inputMode="numeric"
              />
            </label>
            <label>
              Amount
              <input
                value={transferAmount}
                onChange={(e) => setTransferAmount(e.target.value)}
                inputMode="decimal"
              />
            </label>
            <button type="submit" disabled={busy || accounts.length < 1}>
              Send
            </button>
          </form>
          <p className="muted">
            Destination can be another customer&apos;s account id. Each send uses a fresh
            Idempotency-Key.
          </p>

          <h3>Recent transfers</h3>
          {transfers.length === 0 ? (
            <p className="muted">None yet.</p>
          ) : (
            <ul className="plain-list">
              {transfers.slice(0, 10).map((t) => (
                <li key={t.id}>
                  #{t.id} {t.status}: {t.fromAccountId} → {t.toAccountId} · {t.currency}{" "}
                  {money(t.amount)}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
