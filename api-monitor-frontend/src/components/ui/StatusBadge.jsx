import LiveDot from "./LiveDot";

const STYLES = {
  HEALTHY: "bg-success/10 text-success ring-1 ring-inset ring-success/25",
  SLOW: "bg-warning/10 text-warning ring-1 ring-inset ring-warning/25",
  DOWN: "bg-danger/10 text-danger ring-1 ring-inset ring-danger/25",
  UNKNOWN: "bg-text-muted/10 text-text-muted ring-1 ring-inset ring-text-muted/25",
};

const LABELS = {
  HEALTHY: "Healthy",
  SLOW: "Slow",
  DOWN: "Down",
  UNKNOWN: "Unknown",
};

export default function StatusBadge({ status, className = "" }) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-xs font-medium ${
        STYLES[status] || STYLES.UNKNOWN
      } ${className}`}
    >
      <LiveDot status={status} size="sm" />
      {LABELS[status] || status}
    </span>
  );
}
