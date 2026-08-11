const TONES = {
  HEALTHY: "bg-success",
  SLOW: "bg-warning",
  DOWN: "bg-danger",
  UNKNOWN: "bg-text-muted",
};

/**
 * Status dot with an expanding halo. The halo is what makes a live dashboard feel live -
 * a static coloured circle looks identical whether polling is working or frozen.
 */
export default function LiveDot({ status = "UNKNOWN", size = "md", className = "" }) {
  const tone = TONES[status] || TONES.UNKNOWN;
  const dimensions = size === "sm" ? "h-1.5 w-1.5" : "h-2 w-2";

  return (
    <span className={`relative inline-flex ${dimensions} ${className}`}>
      {status !== "UNKNOWN" && (
        <span className={`absolute inline-flex h-full w-full animate-pulse-ring rounded-full ${tone}`} />
      )}
      <span className={`relative inline-flex h-full w-full rounded-full ${tone}`} />
    </span>
  );
}
