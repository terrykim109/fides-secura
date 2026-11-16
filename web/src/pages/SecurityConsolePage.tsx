import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  clearAccessToken,
  decideIncident,
  fetchIncidents,
  fetchMe,
  getAccessToken,
  type IncidentResponse,
  type UserResponse,
} from "../api/client";

export function SecurityConsolePage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [incidents, setIncidents] = useState<IncidentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [incidentNote, setIncidentNote] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadIncidents = useCallback(async () => {
    if (!getAccessToken()) {
      setIncidentNote("Sign in as analyst@fidessecura.bank to review holds.");
      return;
    }
    try {
      const me = await fetchMe();
      setUser(me);
      if (me.role !== "ANALYST" && me.role !== "ADMIN") {
        setIncidentNote(
          `You’re signed in as ${me.role}. Sign out and use the analyst demo account — this desk is for people who decide whether money moves.`,
        );
        setIncidents([]);
        return;
      }
      const list = await fetchIncidents("open");
      setIncidents(list);
      setIncidentNote(
        list.length === 0
          ? "No open holds. Trigger the customer ATO path first (failures → new IP → large send)."
          : null,
      );
    } catch (err: unknown) {
      setIncidentNote(err instanceof Error ? err.message : "Could not load incidents");
    }
  }, []);

  useEffect(() => {
    loadIncidents();
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
            Hold desk
            {user ? ` · ${user.email} · ${user.role}` : ""}
          </p>
        </div>
        <nav className="top-nav">
          <Link className="text-link" to="/app">
            Customer
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
        <h1>Someone may have their login</h1>
        <p className="lede">
          These are payouts we froze before balances changed. Read the timeline —
          approve only if you believe this is still the real owner.
        </p>

        {error && <p className="error">{error}</p>}

        <section className="status-block">
          <h2>Open holds</h2>
          {incidentNote && <p className="muted">{incidentNote}</p>}
          {incidents.map((inc) => (
            <article key={inc.id} className="incident-card">
              <h3>
                #{inc.id} · {inc.title}
              </h3>
              <p>{inc.summary}</p>
              <p className="muted">
                Transfer {inc.transferId ?? "—"} · {inc.severity} · {inc.ruleName}
              </p>
              {inc.timeline && inc.timeline.length > 0 && (
                <>
                  <h4>What we saw</h4>
                  <ol className="timeline">
                    {inc.timeline.map((beat) => (
                      <li key={beat.eventId}>
                        <span className="muted">
                          {new Date(beat.at).toLocaleString()}
                        </span>
                        <br />
                        {beat.plainEnglish}
                      </li>
                    ))}
                  </ol>
                </>
              )}
              <div className="inline-form">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => onDecide(inc.id, "approve")}
                >
                  Release money
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => onDecide(inc.id, "reject")}
                >
                  Kill payout
                </button>
              </div>
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}
