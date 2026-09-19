import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  fetchCapabilities,
  type CapabilitiesResponse,
} from "../api/client";

export function SecurityConsolePage() {
  const [caps, setCaps] = useState<CapabilitiesResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchCapabilities()
      .then((data) => {
        if (!cancelled) setCaps(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load capabilities");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const roles = caps?.rolesPlanned ?? caps?.roles ?? [];

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <p className="brand">Fides Secura</p>
          <p className="context">Analyst shell · sample probes only</p>
        </div>
        <nav className="top-nav">
          <Link className="text-link" to="/app">
            Customer shell
          </Link>
          <Link className="text-link" to="/login">
            Sign out
          </Link>
        </nav>
      </header>

      <main className="content content--wide">
        <h1>Capabilities probe</h1>
        <p className="lede">
          Shows sample risk/correlation helper output. Not wired to live
          auth or transfers yet.
        </p>

        {error && <p className="error">{error}</p>}
        {!caps && !error && <p className="muted">Loading…</p>}

        {caps && (
          <>
            <section className="status-block">
              <h2>Platform</h2>
              <dl className="kv">
                <div>
                  <dt>Product</dt>
                  <dd>{caps.product}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>{caps.status ?? caps.focus ?? "—"}</dd>
                </div>
                <div>
                  <dt>Note</dt>
                  <dd>{caps.note ?? "—"}</dd>
                </div>
                <div>
                  <dt>Roles (planned)</dt>
                  <dd>{roles.length ? roles.join(", ") : "—"}</dd>
                </div>
              </dl>
            </section>

            <section className="status-block">
              <h2>Sample fraud assessment</h2>
              <p className="lede tight">
                Risk score <strong>{caps.fraudSample.riskScore}</strong>
                {caps.fraudSample.flagged ? " — would flag" : " — clear"}
              </p>
              <ul>
                {caps.fraudSample.reasons.map((reason) => (
                  <li key={reason}>{reason}</li>
                ))}
              </ul>
            </section>

            <section className="status-block">
              <h2>Sample correlation preview</h2>
              <dl className="kv">
                <div>
                  <dt>Rule</dt>
                  <dd>{caps.correlationSample.ruleName}</dd>
                </div>
                <div>
                  <dt>Title</dt>
                  <dd>{caps.correlationSample.title}</dd>
                </div>
                <div>
                  <dt>Severity</dt>
                  <dd>{caps.correlationSample.severity}</dd>
                </div>
              </dl>
              <p className="lede tight">{caps.correlationSample.summary}</p>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
