import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  clearAccessToken,
  fetchHealth,
  fetchMe,
  getAccessToken,
  type HealthResponse,
  type UserResponse,
} from "../api/client";

export function DashboardPage() {
  const navigate = useNavigate();
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!getAccessToken()) {
      navigate("/login");
      return;
    }
    Promise.all([fetchHealth(), fetchMe()])
      .then(([healthData, me]) => {
        if (!cancelled) {
          setHealth(healthData);
          setUser(me);
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
          Auth is live. Accounts and transfers are the next slice.
        </p>

        <section className="status-block" aria-live="polite">
          <h2>Session</h2>
          {error && <p className="error">{error}</p>}
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
      </main>
    </div>
  );
}
