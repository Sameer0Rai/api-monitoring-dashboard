import { useEffect, useState } from "react";
import { Globe, Loader2, Timer, X } from "lucide-react";

// The common cadences, so the usual case is one click instead of typing a number. The
// field stays editable for anything in between.
const INTERVAL_PRESETS = [
  { seconds: 30, label: "30s" },
  { seconds: 60, label: "1m" },
  { seconds: 300, label: "5m" },
  { seconds: 900, label: "15m" },
];

export default function AddServiceModal({ open, onClose, onSubmit }) {
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [intervalSeconds, setIntervalSeconds] = useState(60);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  // Esc closes the dialog - expected of anything that dims the page behind it.
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFieldErrors({});

    onSubmit({ name, url, intervalSeconds: Number(intervalSeconds) })
      .then(() => {
        setName("");
        setUrl("");
        setIntervalSeconds(60);
        onClose();
      })
      .catch((err) => setFieldErrors(err.fieldErrors || { _general: err.message }))
      .finally(() => setSubmitting(false));
  };

  return (
    <div
      className="fixed inset-0 z-40 flex animate-fade-in items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-service-title"
        className="card w-full max-w-md animate-slide-up overflow-hidden shadow-lift"
      >
        <div className="relative border-b border-border px-6 py-5">
          <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-brand-500/60 to-transparent" />
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-brand-500/10 p-2 text-brand-fg">
                <Globe className="h-4 w-4" />
              </div>
              <div>
                <h2 id="add-service-title" className="text-base font-semibold text-text-primary">
                  Monitor a new endpoint
                </h2>
                <p className="mt-0.5 text-xs text-text-muted">
                  We'll start checking it on the next scheduler tick.
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="rounded-md p-1 text-text-muted transition-colors hover:bg-surface-hover hover:text-text-primary"
              aria-label="Close"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 p-6">
          <div>
            <label className="label" htmlFor="name">Display name</label>
            <input
              id="name"
              className="input"
              placeholder="Payments API"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
              required
            />
            {fieldErrors.name && <p className="mt-1 text-xs text-danger">{fieldErrors.name}</p>}
          </div>

          <div>
            <label className="label" htmlFor="url">Endpoint URL</label>
            <input
              id="url"
              className="input font-mono text-xs"
              placeholder="https://api.example.com/health"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              required
            />
            {fieldErrors.url ? (
              <p className="mt-1 text-xs text-danger">{fieldErrors.url}</p>
            ) : (
              <p className="mt-1 text-xs text-text-muted">
                A lightweight health route works best - it gets hit a lot.
              </p>
            )}
          </div>

          <div>
            <label className="label" htmlFor="interval">Check interval</label>
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5">
                {INTERVAL_PRESETS.map((preset) => (
                  <button
                    key={preset.seconds}
                    type="button"
                    onClick={() => setIntervalSeconds(preset.seconds)}
                    className={`rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors ${
                      Number(intervalSeconds) === preset.seconds
                        ? "bg-brand-500/15 text-brand-fg"
                        : "text-text-muted hover:text-text-primary"
                    }`}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>

              <div className="relative flex-1">
                <Timer className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-muted" />
                <input
                  id="interval"
                  type="number"
                  min={15}
                  max={3600}
                  className="input !pl-8"
                  value={intervalSeconds}
                  onChange={(e) => setIntervalSeconds(e.target.value)}
                />
              </div>
            </div>
            {fieldErrors.intervalSeconds && (
              <p className="mt-1 text-xs text-danger">{fieldErrors.intervalSeconds}</p>
            )}
          </div>

          {fieldErrors._general && (
            <div className="rounded-lg border border-danger/30 bg-danger/5 px-3 py-2 text-xs text-danger">
              {fieldErrors._general}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={submitting} className="btn-primary">
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {submitting ? "Adding…" : "Start monitoring"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
