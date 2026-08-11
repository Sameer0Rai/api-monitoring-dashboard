const TONES = {
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
  brand: "bg-brand-500",
};

/**
 * Horizontal fill bar for percentage values. Uptime as a bare "99.4%" is hard to scan in
 * a list; a bar makes the outlier row obvious without reading a single digit.
 */
export default function Meter({ value, tone = "brand", className = "" }) {
  const pct = Math.max(0, Math.min(100, Number(value) || 0));

  return (
    <div className={`h-1.5 w-full overflow-hidden rounded-full bg-surface-sunken ${className}`}>
      <div
        className={`h-full rounded-full transition-all duration-500 ${TONES[tone] || TONES.brand}`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}

/** Uptime maps to a tone on a fixed scale so the same number always looks the same. */
export function uptimeTone(uptimePercentage) {
  if (uptimePercentage == null) return "brand";
  if (uptimePercentage >= 99) return "success";
  if (uptimePercentage >= 95) return "warning";
  return "danger";
}
