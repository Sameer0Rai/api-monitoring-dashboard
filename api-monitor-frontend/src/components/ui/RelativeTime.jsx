import { useEffect, useState } from "react";
import { formatRelativeTime } from "../../utils/formatTime";

// Ticks its own re-render every second so "12s ago" counts up live, without making the
// entire dashboard re-render on a 1s timer just to keep one label honest.
export default function RelativeTime({ isoString, className = "" }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  return <span className={className}>{formatRelativeTime(isoString, now)}</span>;
}
