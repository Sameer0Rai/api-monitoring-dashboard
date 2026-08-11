import { useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Gauge,
  LayoutDashboard,
  Plus,
  RefreshCw,
  Search,
  ServerCrash,
  XCircle,
} from "lucide-react";
import AppShell from "../components/layout/AppShell";
import StatCard from "../components/ui/StatCard";
import ServiceRow from "../components/ServiceRow";
import AddServiceModal from "../components/ServiceForm";
import EmptyState from "../components/ui/EmptyState";
import RelativeTime from "../components/ui/RelativeTime";
import GettingStarted from "../components/onboarding/GettingStarted";
import { SkeletonCard, SkeletonRow } from "../components/ui/Skeletons";
import { useServices } from "../hooks/useServices";
import { useToast } from "../context/ToastContext";

const FILTERS = [
  { key: "ALL", label: "All" },
  { key: "HEALTHY", label: "Healthy" },
  { key: "SLOW", label: "Slow" },
  { key: "DOWN", label: "Down" },
];

export default function DashboardPage() {
  const {
    services,
    metricsByService,
    historyByService,
    loading,
    error,
    lastUpdatedAt,
    addService,
    removeService,
    refresh,
  } = useServices();
  const toast = useToast();
  const [modalOpen, setModalOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");

  const summary = useMemo(() => {
    const values = services.map((s) => metricsByService[s.id]).filter(Boolean);
    const online = values.filter((m) => m.status === "HEALTHY" || m.status === "SLOW").length;
    const offline = values.filter((m) => m.status === "DOWN").length;
    const latencies = values.map((m) => m.averageResponseTimeMs).filter((v) => v != null);
    const avgLatency = latencies.length ? latencies.reduce((a, b) => a + b, 0) / latencies.length : null;
    const uptimes = values.map((m) => m.uptimePercentage);
    const avgUptime = uptimes.length ? uptimes.reduce((a, b) => a + b, 0) / uptimes.length : null;

    return { total: services.length, online, offline, avgLatency, avgUptime };
  }, [services, metricsByService]);

  // One shared latency series across every service - gives the "avg response time" card
  // a shape to show instead of a lone number with nothing to compare it to.
  const latencyTrend = useMemo(() => {
    const series = services.map((s) => historyByService[s.id] || []).filter((h) => h.length);
    if (!series.length) return [];
    const points = Math.min(...series.map((h) => h.length));

    return Array.from({ length: points }, (_, i) => {
      const offset = (h) => h[h.length - points + i]?.responseTimeMs;
      const samples = series.map(offset).filter((v) => v != null);
      return samples.length ? samples.reduce((a, b) => a + b, 0) / samples.length : null;
    }).filter((v) => v != null);
  }, [services, historyByService]);

  const visibleServices = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return services.filter((service) => {
      const status = metricsByService[service.id]?.status || "UNKNOWN";
      if (filter !== "ALL" && status !== filter) return false;
      if (!needle) return true;
      return (
        service.name.toLowerCase().includes(needle) || service.url.toLowerCase().includes(needle)
      );
    });
  }, [services, metricsByService, query, filter]);

  const handleAdd = (payload) =>
    addService(payload).then(() => toast.success(`${payload.name} is now being monitored`));

  const handleDelete = (service) => {
    if (!window.confirm(`Remove ${service.name}? This deletes its monitoring history too.`)) return;
    removeService(service.id)
      .then(() => toast.success(`${service.name} removed`))
      .catch((err) => toast.error(err.message));
  };

  const isEmpty = !loading && services.length === 0;

  return (
    <AppShell title="Dashboard" subtitle="Live overview" icon={LayoutDashboard}>
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-text-primary">
              {isEmpty ? "Welcome aboard" : "Everything you're monitoring"}
            </h2>
            <p className="mt-0.5 flex items-center gap-1.5 text-xs text-text-muted">
              {lastUpdatedAt ? (
                <>
                  <span className="inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-success" />
                  Updated <RelativeTime isoString={new Date(lastUpdatedAt).toISOString()} /> · refreshes
                  every 10s
                </>
              ) : (
                "Loading…"
              )}
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button onClick={refresh} className="btn-secondary !px-2.5" aria-label="Refresh now" title="Refresh now">
              <RefreshCw className="h-4 w-4" />
            </button>
            <button onClick={() => setModalOpen(true)} className="btn-primary">
              <Plus className="h-4 w-4" />
              Add service
            </button>
          </div>
        </div>

        {error && (
          <div className="card flex items-start gap-3 border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">
            <ServerCrash className="mt-0.5 h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {isEmpty ? (
          <GettingStarted onAddPreset={handleAdd} onOpenForm={() => setModalOpen(true)} />
        ) : (
          <>
            {/* Widgets */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => <SkeletonCard key={i} />)
              ) : (
                <>
                  <StatCard
                    label="Total services"
                    value={summary.total}
                    icon={Activity}
                    subtext={`${summary.total - summary.online - summary.offline} awaiting first check`}
                  />
                  <StatCard
                    label="Online"
                    value={summary.online}
                    icon={CheckCircle2}
                    tone="success"
                    subtext={summary.total ? `${Math.round((summary.online / summary.total) * 100)}% of fleet` : null}
                  />
                  <StatCard
                    label="Offline"
                    value={summary.offline}
                    icon={XCircle}
                    tone="danger"
                    subtext={summary.offline ? "Needs attention" : "All clear"}
                  />
                  <StatCard
                    label="Avg response time"
                    value={summary.avgLatency != null ? `${Math.round(summary.avgLatency)} ms` : "—"}
                    icon={Gauge}
                    subtext="Across all services"
                    trend={latencyTrend}
                  />
                  <StatCard
                    label="Avg uptime"
                    value={summary.avgUptime != null ? `${summary.avgUptime.toFixed(1)}%` : "—"}
                    icon={AlertTriangle}
                    tone="warning"
                    subtext="Lifetime of each check"
                  />
                </>
              )}
            </div>

            {/* Service list */}
            <div className="card overflow-hidden">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-3.5">
                <div className="flex items-center gap-2">
                  <h3 className="text-sm font-semibold text-text-primary">Monitored services</h3>
                  <span className="chip !px-2 !py-0.5">{services.length}</span>
                </div>

                <div className="flex flex-1 items-center justify-end gap-2">
                  <div className="relative max-w-[220px] flex-1">
                    <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-muted" />
                    <input
                      className="input !py-1.5 !pl-8 !text-xs"
                      placeholder="Search name or URL"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                      aria-label="Search services"
                    />
                  </div>

                  <div className="hidden items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5 sm:flex">
                    {FILTERS.map((f) => (
                      <button
                        key={f.key}
                        onClick={() => setFilter(f.key)}
                        className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                          filter === f.key
                            ? "bg-brand-500/15 text-brand-fg"
                            : "text-text-muted hover:text-text-primary"
                        }`}
                      >
                        {f.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Column headers - the row is dense enough now that the numbers need labels. */}
              {!loading && visibleServices.length > 0 && (
                <div className="hidden items-center gap-4 border-b border-border bg-surface-sunken/50 px-5 py-2 text-[10px] font-medium uppercase tracking-wide text-text-muted sm:flex">
                  <span className="w-9 shrink-0" />
                  <span className="flex-1">Service</span>
                  <span className="hidden w-20 shrink-0 lg:block">Latency trend</span>
                  <span className="w-20 shrink-0 text-right">Response</span>
                  <span className="hidden w-24 shrink-0 text-right md:block">Uptime</span>
                  <span className="hidden w-24 shrink-0 text-right xl:block">Last check</span>
                  <span className="w-[86px] shrink-0">Status</span>
                  <span className="w-[60px] shrink-0" />
                </div>
              )}

              {loading ? (
                <div>
                  {Array.from({ length: 3 }).map((_, i) => <SkeletonRow key={i} />)}
                </div>
              ) : visibleServices.length === 0 ? (
                <EmptyState
                  icon={Search}
                  title="No services match"
                  description="Try a different search term or clear the status filter."
                  action={
                    <button
                      onClick={() => { setQuery(""); setFilter("ALL"); }}
                      className="btn-secondary mt-2"
                    >
                      Clear filters
                    </button>
                  }
                />
              ) : (
                <div>
                  {visibleServices.map((service) => (
                    <ServiceRow
                      key={service.id}
                      service={service}
                      metrics={metricsByService[service.id]}
                      history={historyByService[service.id]}
                      onDelete={handleDelete}
                    />
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>

      <AddServiceModal open={modalOpen} onClose={() => setModalOpen(false)} onSubmit={handleAdd} />
    </AppShell>
  );
}
