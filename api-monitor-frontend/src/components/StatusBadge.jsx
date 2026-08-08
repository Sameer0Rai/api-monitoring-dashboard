const STYLES = {
  HEALTHY: "badge green",
  SLOW: "badge yellow",
  DOWN: "badge red",
  UNKNOWN: "badge neutral",
};

export default function StatusBadge({ status }) {
  return <span className={STYLES[status] || "badge red"}>{status}</span>;
}
