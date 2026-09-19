import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("analyst@fidessecura.bank");
  const [password, setPassword] = useState("");

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    navigate("/security");
  }

  return (
    <div className="shell shell--auth">
      <header className="brand-bar">
        <p className="brand">Fides Secura</p>
        <p className="brand-sub">Foundation scaffold — auth not wired</p>
      </header>

      <main className="auth-panel">
        <h1>Sign in</h1>
        <p className="lede">
          UI shell only. Real JWT login lands with the auth vertical slice.
        </p>

        <form className="form" onSubmit={onSubmit}>
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
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          <button type="submit">Continue to console shell</button>
        </form>

        <p className="muted">
          <Link to="/security">Security console</Link>
          {" · "}
          <Link to="/app">Customer shell</Link>
        </p>
      </main>
    </div>
  );
}
