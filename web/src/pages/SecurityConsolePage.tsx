import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  clearAccessToken,
  decideIncident,
  fetchCapabilities,
  fetchIncidents,
  fetchMe,
  getAccessToken,
  type CapabilitiesResponse,
  type IncidentResponse,
  type UserResponse,
} from "../api/client";

export function SecurityConsolePage() {
  const [caps, setCaps] = useState<CapabilitiesResponse | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [incidents, setIncidents] = useState<IncidentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [incidentNote, setIncidentNote] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadIncidents = useCallback(async () => {
    if (!getAccessToken()) {
      setIncidentNote("Sign in, then open this page with an ANALYST or ADMIN token.");
      return;
    }
    try {
      const me = await fetchMe();
      setUser(me);
      if (me.role !== "ANALYST" && me.role !== "ADMIN") {
        setIncidentNote(`Signed in as ${me.role}. Promote a user to ANALYST in SQL to review holds.`);
        setIncidents([]);
        return;
      }
      const list = await fetchIncidents("open");
      setIncidents(list);
      setIncidentNote(list.length === 0 ? "No open incidents." : null);
    } catch (err: unknown) {
      setIncidentNote(err instanceof Error ? err.message : "Could not load incidents");
    }
  }, []);

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
    loadIncidents();
    return () => {
      cancelled = true;
    };
  }, [loadIncidents]);

  async function onDecide(id: number, decision: "approve" | "reject") {
    setBusy(true);
    setError(null);
    try {
      await decideIncident(id, decision);
      await loadIncidents();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Decision failed");
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
            Analyst shell
            {user ? ` · ${user.email} · ${user.role}` : ""}
          </p>
        </div>
        <nav className="top-nav">
          <Link className="text-link" to="/app">
            Customer shell
          </Link>
          <Link
            className="text-link"
            to="/login"
            onClick={() => clearAccessToken()}
          >
            Sign out
          </Link>
        </nav>
      </header>

      <main className="content content--wide">
        <h1>Security console</h1>
        <p className="lede">
          Live ATO holds appear below. Capabilities probe remains available for
          quick health of helper modules.
        </p>

        {error && <p className="error">{error}</p>}

        <section className="status-block">
          <h2>Open incidents</h2>
          {incidentNote && <p className="muted">{incidentNote}</p>}
          {incidents.length > 0 && (
            <ul className="plain-list">
              {incidents.map((inc) => (
                <li key={inc.id}>
                  <strong>#{inc.id}</strong> [{inc.severity}] {inc.title} — transfer{" "}
                  {inc.transferId ?? "—"}
                  <br />
                  <span className="muted">{inc.summary}</span>
                  <div className="inline-form">
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => onDecide(inc.id, "approve")}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => onDecide(inc.id, "reject")}
                    >
                      Reject
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

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
              <h2>Correlation rule</h2>
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
