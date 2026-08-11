import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Activity, CheckCircle2 } from "lucide-react";
import { resetPassword } from "../api/auth";
import { useToast } from "../context/ToastContext";

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();
  const toast = useToast();

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setError("Passwords don't match");
      return;
    }
    setSubmitting(true);
    setError(null);
    resetPassword(token, password)
      .then(() => setDone(true))
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
          <h1 className="text-lg font-semibold text-text-primary">Set a new password</h1>
        </div>

        {!token ? (
          <div className="card p-6 text-center text-sm text-danger">
            This reset link is missing its token. Request a new one from the{" "}
            <Link to="/forgot-password" className="font-medium text-brand-500 hover:text-brand-fg">
              forgot password
            </Link>{" "}
            page.
          </div>
        ) : done ? (
          <div className="card flex flex-col items-center gap-3 p-6 text-center">
            <div className="rounded-full bg-success/10 p-2.5 text-success">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <p className="text-sm text-text-primary">Your password has been reset.</p>
            <button
              onClick={() => {
                toast.success("Password reset - sign in with your new password");
                navigate("/login");
              }}
              className="btn-primary w-full"
            >
              Continue to sign in
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="card space-y-4 p-6">
            <div>
              <label className="label" htmlFor="password">New password</label>
              <input
                id="password"
                type="password"
                className="input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={8}
                required
                autoFocus
              />
              <p className="mt-1 text-xs text-text-muted">At least 8 characters.</p>
            </div>
            <div>
              <label className="label" htmlFor="confirmPassword">Confirm new password</label>
              <input
                id="confirmPassword"
                type="password"
                className="input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                minLength={8}
                required
              />
            </div>

            {error && <p className="text-sm text-danger">{error}</p>}

            <button type="submit" disabled={submitting} className="btn-primary w-full">
              {submitting ? "Resetting..." : "Reset password"}
            </button>
          </form>
        )}

        <p className="mt-4 text-center text-sm text-text-secondary">
          <Link to="/login" className="font-medium text-brand-500 hover:text-brand-fg">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
