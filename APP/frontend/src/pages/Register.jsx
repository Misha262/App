import { useState } from "react";
import { apiPost } from "../services/api";

export default function Register({ goLogin }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess(false);
    setLoading(true);

    try {
      // backend register — переменная нам не нужна → просто await
      await apiPost("/api/auth/register", {
        name,
        email,
        password,
      });

      setSuccess(true);

      setTimeout(() => goLogin(), 500);
    } catch (err) {
      console.error("Register error:", err);

      let msg = "Registration failed";

      // безопасный разбор ошибки
      try {
        const backendJson = err?.message?.replace("Server returned error: ", "");
        const parsed = JSON.parse(backendJson);

        if (parsed?.error) msg = parsed.error;
      } catch {
        // eslint не ругается, потому что catch без параметров
      }

      if (msg.includes("email")) msg = "Email already in use";

      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">

        <div className="auth-main">
          <div className="auth-title">Create account ✨</div>
          <div className="auth-subtitle">
            Join study groups, track tasks and chat in real time.
          </div>

          <form className="auth-form" onSubmit={submit}>
            <div>
              <div className="label">Name</div>
              <input
                className="input"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <div>
              <div className="label">Email</div>
              <input
                className="input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <div>
              <div className="label">Password</div>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            {error && <div className="error">{error}</div>}
            {success && <div className="success">✓ Account created! Redirecting…</div>}

            <button
              className="btn-primary"
              type="submit"
              disabled={loading}
              style={{ opacity: loading ? 0.6 : 1 }}
            >
              {loading ? "Creating…" : "➜ Create account"}
            </button>
          </form>

          <div className="auth-footer">
            Already have an account?{" "}
            <button type="button" onClick={goLogin} disabled={loading}>
              Log in
            </button>
          </div>
        </div>

        <div className="auth-side">
          <div>
            <div className="side-small-title">Why register?</div>
            <div className="side-big-title">Join your study flow.</div>
            <div className="side-desc">
              Create and join groups, manage tasks, track deadlines and chat
              instantly with your classmates.
            </div>
          </div>

          <div className="deadline-box">
            <span>Popular feature:</span>
            <span>Real-time group chat 💬</span>
          </div>
        </div>

      </div>
    </div>
  );
}
