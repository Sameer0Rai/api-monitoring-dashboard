import { useMemo } from "react";
import { Link } from "react-router-dom";
import { ChevronRight, Trash2 } from "lucide-react";
import StatusBadge from "./ui/StatusBadge";
import RelativeTime from "./ui/RelativeTime";
import Sparkline from "./ui/Sparkline";
import Meter, { uptimeTone } from "./ui/Meter";

const SPARK_STROKE = {
  HEALTHY: "rgb(34 197 94)",
  SLOW: "rgb(245 158 11)",
  DOWN: "rgb(239 68 68)",
  UNKNOWN: "rgb(100 116 139)",
};

// The host is the part that identifies a service at a glance; the full URL with its
// /v2/internal/health tail is noise in a dense list.
function hostOf(url) {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
}

export default function ServiceRow({ service, metrics, history = [], onDelete }) {
  const status = metrics?.status || "UNKNOWN";
  const latencies = useMemo(
    () => history.map((log) => log.responseTimeMs).filter((v) => v != null),
    [history]
  );

  return (
    <div className="group relative flex items-center gap-4 border-b border-border px-5 py-3.5 transition-colors last:border-b-0 hover:bg-surface-hover">
      {/* Status stripe on the leading edge - lets you find the broken service by colour
          before reading any text. */}
      <span
        className={`absolute inset-y-0 left-0 w-0.5 transition-opacity ${
          status === "DOWN" ? "bg-danger" : status === "SLOW" ? "bg-warning" : "bg-success"
        } ${status === "UNKNOWN" ? "opacity-0" : "opacity-70"}`}
      />

      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-brand-500/20 to-accent-500/10 text-sm font-semibold uppercase text-brand-fg">
        {service.name?.[0] || "?"}
      </div>

      <Link to={`/services/${service.id}`} className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-text-primary group-hover:text-brand-fg">
          {service.name}
        </p>
        <p className="truncate font-mono text-[11px] text-text-muted">{hostOf(service.url)}</p>
      </Link>

      <Sparkline
        values={latencies}
        width={80}
        height={26}
        stroke={SPARK_STROKE[status]}
        className="hidden shrink-0 lg:block"
      />

      <div className="hidden w-20 shrink-0 text-right sm:block">
        <p className="text-sm tabular-nums text-text-primary">
          {metrics?.averageResponseTimeMs != null ? `${Math.round(metrics.averageResponseTimeMs)}` : "—"}
          {metrics?.averageResponseTimeMs != null && (
            <span className="ml-0.5 text-[11px] text-text-muted">ms</span>
          )}
        </p>
        <p className="text-[10px] uppercase tracking-wide text-text-muted">avg</p>
      </div>

      <div className="hidden w-24 shrink-0 md:block">
        <p className="text-right text-sm tabular-nums text-text-primary">
          {metrics ? `${metrics.uptimePercentage.toFixed(1)}%` : "—"}
        </p>
        <Meter
          value={metrics?.uptimePercentage ?? 0}
          tone={uptimeTone(metrics?.uptimePercentage)}
          className="mt-1.5"
        />
      </div>

      <div className="hidden w-24 shrink-0 text-right text-xs text-text-muted xl:block">
        <RelativeTime isoString={service.lastCheckedAt} />
      </div>

      {/* Fixed width so the badge column lines up with its header regardless of label. */}
      <div className="w-[86px] shrink-0">
        <StatusBadge status={status} />
      </div>

      <div className="flex shrink-0 items-center gap-1">
        <button
          onClick={() => onDelete(service)}
          className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-danger/10 hover:text-danger"
          aria-label={`Delete ${service.name}`}
          title="Delete service"
        >
          <Trash2 className="h-4 w-4" />
        </button>
        <Link
          to={`/services/${service.id}`}
          className="rounded-md p-1.5 text-text-muted transition-colors hover:text-text-primary"
          aria-label={`Open ${service.name}`}
        >
          <ChevronRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}
