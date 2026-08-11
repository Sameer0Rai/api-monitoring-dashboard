import { useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Activity, ArrowLeft, Clock, Gauge, RefreshCw, TrendingDown, TrendingUp } from "lucide-react";
import AppShell from "../components/layout/AppShell";
import StatCard from "../components/ui/StatCard";
import StatusBadge from "../components/ui/StatusBadge";
import RelativeTime from "../components/ui/RelativeTime";
import ResponseTimeChart from "../components/charts/ResponseTimeChart";
import UptimeTrendChart from "../components/charts/UptimeTrendChart";
import { SkeletonChart, SkeletonCard } from "../components/ui/Skeletons";
import { useServiceDetail } from "../hooks/useServiceDetail";

export default function ServiceDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { service, logs, metrics, analytics, loading, error, lastUpdatedAt, refresh } = useServiceDetail(id, 30);

  const latencyData = useMemo(
    () => logs.map((log) => ({ time: log.checkedAt, latency: log.responseTimeMs })),
    [logs]
  );

  const uptimeData = useMemo(
    () => logs.map((log) => ({ time: log.checkedAt, up: log.success ? 1 : 0 })),
    [logs]
  );

  const recentFailures = useMemo(
    () => logs.filter((log) => !log.success).slice(-10).reverse(),
    [logs]
  );

  return (
    <AppShell title="Service details" subtitle={service?.name} icon={Activity}>
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate("/")}
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to dashboard
          </button>

          <div className="flex items-center gap-3">
            <p className="text-xs text-text-muted">
              {lastUpdatedAt ? (
                <>
                  Updated <RelativeTime isoString={new Date(lastUpdatedAt).toISOString()} />
                </>
              ) : (
                "Loading…"
              )}
            </p>
            <button onClick={refresh} className="btn-secondary" aria-label="Refresh now" title="Refresh now">
              <RefreshCw className="h-4 w-4" />
            </button>
          </div>
        </div>

        {error && (
          <div className="card border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">{error}</div>
        )}

        {loading ? (
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : (
          metrics && (
            <>
              <div className="flex items-center gap-3">
                <StatusBadge status={metrics.status} />
                <div className="min-w-0">
                  <h2 className="truncate text-lg font-semibold text-text-primary">
                    {service?.name || `Service #${id}`}
                  </h2>
                  {service?.url && (
                    <p className="truncate text-sm text-text-secondary">{service.url}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <StatCard
                  label="Uptime (30d)"
                  value={`${analytics?.uptimePercentage.toFixed(2) ?? metrics.uptimePercentage.toFixed(2)}%`}
                  icon={TrendingUp}
                  tone="success"
                />
                <StatCard
                  label="Avg response time"
                  value={metrics.averageResponseTimeMs != null ? `${Math.round(metrics.averageResponseTimeMs)} ms` : "—"}
                  icon={Gauge}
                />
                <StatCard
                  label="Total outages (30d)"
                  value={analytics?.totalOutages ?? "—"}
                  icon={TrendingDown}
                  tone="danger"
                />
                <StatCard
                  label="Avg recovery time"
                  value={analytics?.averageRecoverySeconds != null ? `${Math.round(analytics.averageRecoverySeconds)}s` : "—"}
                  icon={Clock}
                  tone="warning"
                />
              </div>
            </>
          )
        )}

        <div className="grid gap-4 lg:grid-cols-2">
          {loading ? (
            <>
              <SkeletonChart />
              <SkeletonChart />
            </>
          ) : (
            <>
              <div className="card p-5">
                <h3 className="mb-4 text-sm font-semibold text-text-primary">Latency trend</h3>
                <ResponseTimeChart data={latencyData} />
              </div>
              <div className="card p-5">
                <h3 className="mb-4 text-sm font-semibold text-text-primary">Uptime trend</h3>
                <UptimeTrendChart data={uptimeData} />
              </div>
            </>
          )}
        </div>

        {analytics && (
          <div className="card p-5">
            <h3 className="mb-4 text-sm font-semibold text-text-primary">30-day analytics</h3>
            <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3 lg:grid-cols-6">
              <Stat label="Total checks" value={analytics.totalChecks} />
              <Stat label="Fastest" value={analytics.fastestResponseTimeMs != null ? `${analytics.fastestResponseTimeMs} ms` : "—"} />
              <Stat label="Slowest" value={analytics.slowestResponseTimeMs != null ? `${analytics.slowestResponseTimeMs} ms` : "—"} />
              <Stat label="Uptime" value={`${analytics.uptimePercentage.toFixed(2)}%`} />
              <Stat label="Downtime" value={`${analytics.downtimePercentage.toFixed(2)}%`} />
              <Stat label="Outages" value={analytics.totalOutages} />
            </div>
          </div>
        )}

        <div className="card overflow-hidden">
          <div className="border-b border-border px-5 py-4">
            <h3 className="text-sm font-semibold text-text-primary">Recent failures</h3>
          </div>
          {recentFailures.length === 0 ? (
            <p className="px-5 py-8 text-center text-sm text-text-muted">No failures in the loaded history. Nice.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-text-muted">
                  <th className="px-5 py-3 font-medium">Status code</th>
                  <th className="px-5 py-3 font-medium">Response time</th>
                  <th className="px-5 py-3 font-medium">Checked at</th>
                </tr>
              </thead>
              <tbody>
                {recentFailures.map((log) => (
                  <tr key={log.id} className="border-b border-border last:border-b-0">
                    <td className="px-5 py-3 text-danger">{log.statusCode || "No response"}</td>
                    <td className="px-5 py-3 text-text-secondary">{log.responseTimeMs} ms</td>
                    <td className="px-5 py-3 text-text-secondary">
                      {new Date(log.checkedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </AppShell>
  );
}

function Stat({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-muted">{label}</p>
      <p className="mt-1 font-semibold text-text-primary">{value}</p>
    </div>
  );
}
