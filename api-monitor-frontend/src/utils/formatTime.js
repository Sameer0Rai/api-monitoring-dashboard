// Short axis tick: just the time, since a service's chart window is usually hours,
// not days - a full date on every tick would be noisy repetition.
export function formatTick(isoString) {
  const date = new Date(isoString);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

// Fuller label for tooltips, where there's room and the date matters if the window
// happens to span midnight.
export function formatTooltipLabel(isoString) {
  const date = new Date(isoString);
  return date.toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

// "Just now" / "12s ago" / "3m ago" style label - the visible proof that polling is
// actually landing fresh data, not just a static number that happens not to have moved.
export function formatRelativeTime(isoString, nowMs = Date.now()) {
  if (!isoString) return "never";

  const diffMs = nowMs - new Date(isoString).getTime();
  if (diffMs < 0) return "just now";

  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 5) return "just now";
  if (diffSec < 60) return `${diffSec}s ago`;

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;

  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;

  const diffDay = Math.floor(diffHr / 24);
  return `${diffDay}d ago`;
}
