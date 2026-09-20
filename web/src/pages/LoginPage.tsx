import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, register, setAccessToken } from "../api/client";

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("customer@fidessecura.bank");
  const [password, setPassword] = useState("");
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
      navigate(result.user.role === "ANALYST" || result.user.role === "ADMIN" ? "/security" : "/app");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shell shell--auth">
      <header className="brand-bar">
        <p className="brand">Fides Secura</p>
        <p className="brand-sub">JWT auth · lockout · security events</p>
      </header>

      <main className="auth-panel">
        <h1>{mode === "login" ? "Sign in" : "Create account"}</h1>
        <p className="lede">
          Register a customer, then sign in. Five failed logins lock the
          account temporarily and write security events.
        </p>

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

        <p className="muted">
          <Link to="/security">Security console</Link>
          {" · "}
          <Link to="/app">Customer shell</Link>
        </p>
      </main>
    </div>
  );
}
