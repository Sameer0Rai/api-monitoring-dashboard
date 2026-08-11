import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Activity } from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    login(email, password)
      .then(() => navigate("/"))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm animate-fade-in">
        <div className="mb-8 flex flex-col items-center gap-2">
          <div className="rounded-xl bg-brand-600 p-2.5">
            <Activity className="h-5 w-5 text-white" />
          </div>
          <h1 className="text-lg font-semibold text-text-primary">Welcome back</h1>
          <p className="text-sm text-text-secondary">Sign in to your monitoring dashboard</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4 p-6">
          <div>
            <label className="label" htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              className="input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoFocus
            />
          </div>
          <div>
            <div className="flex items-center justify-between">
              <label className="label" htmlFor="password">Password</label>
              <Link to="/forgot-password" className="text-xs font-medium text-brand-500 hover:text-brand-fg">
                Forgot password?
              </Link>
            </div>
            <input
              id="password"
              type="password"
              className="input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          <button type="submit" disabled={submitting} className="btn-primary w-full">
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-text-secondary">
          Don't have an account?{" "}
          <Link to="/register" className="font-medium text-brand-500 hover:text-brand-fg">
            Create one
          </Link>
        </p>
      </div>
    </div>
  );
}
