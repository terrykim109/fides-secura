import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, register, setAccessToken } from "../api/client";

const DEMO = {
  customer: { email: "customer@fidessecura.bank", password: "password1" },
  analyst: { email: "analyst@fidessecura.bank", password: "password1" },
} as const;

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState<string>(DEMO.customer.email);
  const [password, setPassword] = useState<string>(DEMO.customer.password);
  const [fullName, setFullName] = useState("Demo Customer");
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const result =
        mode === "login"
          ? await login(email, password)
          : await register(email, password, fullName);
      setAccessToken(result.accessToken);
      navigate(
        result.user.role === "ANALYST" || result.user.role === "ADMIN"
          ? "/security"
          : "/app",
      );
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  function fillDemo(kind: keyof typeof DEMO) {
    setMode("login");
    setEmail(DEMO[kind].email);
    setPassword(DEMO[kind].password);
    setError(null);
  }

  return (
    <div className="shell shell--auth">
      <header className="brand-bar">
        <p className="brand">Fides Secura</p>
        <p className="brand-sub">Transfers · lockout · ATO hold</p>
      </header>

      <main className="auth-panel">
        <h1>{mode === "login" ? "Sign in" : "Create account"}</h1>
        <p className="lede">
          Built around a simple fear: once someone else has the login, they can
          move money. Use the customer account to try a payout; use the analyst
          account to see (and stop) a held transfer.
        </p>

        <div className="demo-chips">
          <button type="button" className="chip" onClick={() => fillDemo("customer")}>
            Fill customer
          </button>
          <button type="button" className="chip" onClick={() => fillDemo("analyst")}>
            Fill analyst
          </button>
        </div>

        <form className="form" onSubmit={onSubmit}>
          {mode === "register" && (
            <label>
              Full name
              <input
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </label>
          )}
          <label>
            Email
            <input
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
            />
          </label>
          <button type="submit" disabled={busy}>
            {busy ? "Please wait…" : mode === "login" ? "Sign in" : "Register"}
          </button>
        </form>

        {error && <p className="error">{error}</p>}

        <p className="muted">
          {mode === "login" ? (
            <>
              No account?{" "}
              <button type="button" className="linkish" onClick={() => setMode("register")}>
                Register
              </button>
            </>
          ) : (
            <>
              Have an account?{" "}
              <button type="button" className="linkish" onClick={() => setMode("login")}>
                Sign in
              </button>
            </>
          )}
        </p>

        <section className="status-block">
          <h2>The story this app is trying to catch</h2>
          <ol className="plain-list">
            <li>Someone hammers the password (3+ failures).</li>
            <li>They get in from a new IP — not your usual home network.</li>
            <li>They try to send ≥ $5,000 out. We hold it instead of completing.</li>
            <li>An analyst sees the incident and decides: release or kill.</li>
          </ol>
          <p className="muted">
            Seeded: customer &amp; merchant &amp; analyst — all{" "}
            <code>password1</code>. Customer starts with ~$10k.
          </p>
        </section>

        <p className="muted">
          <Link to="/security">Security console</Link>
          {" · "}
          <Link to="/app">Customer shell</Link>
        </p>
      </main>
    </div>
  );
}
