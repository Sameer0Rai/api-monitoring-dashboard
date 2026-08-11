import { useState } from "react";
import { Link } from "react-router-dom";
import { Activity, ArrowLeft, MailCheck } from "lucide-react";
import { forgotPassword } from "../api/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    forgotPassword(email)
      .then(() => setSent(true))
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
          <h1 className="text-lg font-semibold text-text-primary">Reset your password</h1>
          <p className="text-center text-sm text-text-secondary">
            Enter your account email and we'll send you a reset link
          </p>
        </div>

        {sent ? (
          <div className="card flex flex-col items-center gap-3 p-6 text-center">
            <div className="rounded-full bg-success/10 p-2.5 text-success">
              <MailCheck className="h-5 w-5" />
            </div>
            <p className="text-sm text-text-primary">
              If an account exists for <span className="font-medium">{email}</span>, a reset
              link is on its way.
            </p>
            <p className="text-xs text-text-muted">Check your inbox and spam folder.</p>
          </div>
        ) : (
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

            {error && <p className="text-sm text-danger">{error}</p>}

            <button type="submit" disabled={submitting} className="btn-primary w-full">
              {submitting ? "Sending link..." : "Send reset link"}
            </button>
          </form>
        )}

        <p className="mt-4 text-center text-sm text-text-secondary">
          <Link to="/login" className="inline-flex items-center gap-1 font-medium text-brand-500 hover:text-brand-fg">
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
