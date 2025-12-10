import { useState } from "react";
import { apiPost } from "../services/api";

export default function Login({ onLogin, goRegister }) {
  const [email, setEmail] = useState("usercomfy171220187@gmail.com");
  const [password, setPassword] = useState("tester");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleOAuth = (provider) => {
    setError("");
    setSuccess(false);
    setLoading(true);
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess(false);
    setLoading(true);

    try {
      const data = await apiPost("/api/auth/login", { email, password });

      if (!data?.token || !data?.user) {
        setError("Invalid credentials");
        setLoading(false);
        return;
      }

      localStorage.setItem("token", data.token);
      localStorage.setItem("user", JSON.stringify(data.user));

      setSuccess(true);

      setTimeout(() => {
        onLogin(data.user);
      }, 500);
    } catch (err) {
      console.error("Login error:", err);
      setError("Login failed: wrong email or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div className="auth-main">
          <div className="auth-title">Welcome back 👋</div>
          <div className="auth-subtitle">
            Log in to access your groups, tasks and real-time chat.
          </div>

          <form className="auth-form" onSubmit={submit}>
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
            {success && (
              <div className="success">Login successful, redirecting…</div>
            )}

            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
              style={{ opacity: loading ? 0.6 : 1 }}
            >
              {loading ? "Checking…" : "→ Enter dashboard"}
            </button>
          </form>

          <div className="auth-or">
            <span />
            or continue with
            <span />
          </div>

          <div className="auth-oauth">
            <button
              type="button"
              className="oauth-btn google"
              onClick={() => handleOAuth("google")}
              disabled={loading}
            >
              <span className="oauth-icon">G</span>
              Continue with Google
            </button>
            <button
              type="button"
              className="oauth-btn apple"
              onClick={() => handleOAuth("apple")}
              disabled={loading}
            >
              <span className="oauth-icon"></span>
              Continue with Apple
            </button>
          </div>

          <div className="auth-footer">
            No account yet?{" "}
            <button type="button" onClick={goRegister}>
              Create one
            </button>
          </div>
        </div>

        <div className="auth-side">
          <div>
            <div className="side-small-title">Today overview</div>
            <div className="side-big-title">
              Keep your study flow organized.
            </div>
            <div className="side-desc">
              Track group activity, deadlines and shared resources in one clean
              dashboard. Chat in real time and never miss an important update.
            </div>
          </div>

          <div className="deadline-box">
            <span>Next deadline: Algorithms Group</span>
            <span>Today • 23:59</span>
          </div>
        </div>
      </div>
    </div>
  );
}
