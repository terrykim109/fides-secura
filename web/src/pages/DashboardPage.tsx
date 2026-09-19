import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchHealth, type HealthResponse } from "../api/client";

export function DashboardPage() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchHealth()
      .then((data) => {
        if (!cancelled) setHealth(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to reach API");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <p className="brand">Fides Secura</p>
          <p className="context">Customer shell · accounts not implemented</p>
        </div>
        <nav className="top-nav">
          <Link className="text-link" to="/security">
            Security console
          </Link>
          <Link className="text-link" to="/login">
            Sign out
          </Link>
        </nav>
      </header>

      <main className="content">
        <h1>Workspace</h1>
        <p className="lede">
          Placeholder until accounts and transfers ship. Health check only.
        </p>

        <section className="status-block" aria-live="polite">
          <h2>API status</h2>
          {error && <p className="error">{error}</p>}
          {health && (
            <dl className="kv">
              <div>
                <dt>Status</dt>
                <dd>{health.status}</dd>
              </div>
              <div>
                <dt>Service</dt>
                <dd>{health.service}</dd>
              </div>
              <div>
                <dt>Focus</dt>
                <dd>{health.focus ?? "—"}</dd>
              </div>
            </dl>
          )}
          {!health && !error && <p className="muted">Checking API…</p>}
        </section>
      </main>
    </div>
  );
}
