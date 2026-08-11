import { useState } from "react";
import {
  ArrowRight,
  Github,
  Globe,
  LineChart,
  Loader2,
  Plus,
  Radar,
  ShieldCheck,
  Timer,
  TrendingUp,
  Zap,
} from "lucide-react";
import Sparkline from "../ui/Sparkline";
import LiveDot from "../ui/LiveDot";
import Meter from "../ui/Meter";
import { API_ORIGIN } from "../../config/env";

// The monitor can watch its own backend - the one endpoint we know is reachable from
// wherever this app is running, which makes it the safest possible first service.
const SELF_HEALTH_URL = `${API_ORIGIN}/actuator/health`;

const PRESETS = [
  {
    name: "This monitor's API",
    url: SELF_HEALTH_URL,
    intervalSeconds: 30,
    icon: ShieldCheck,
    blurb: "Your own backend's health probe",
  },
  {
    name: "GitHub API",
    url: "https://api.github.com",
    intervalSeconds: 60,
    icon: Github,
    blurb: "A busy public REST API",
  },
  {
    name: "JSONPlaceholder",
    url: "https://jsonplaceholder.typicode.com/posts/1",
    intervalSeconds: 60,
    icon: Globe,
    blurb: "Stable demo endpoint",
  },
  {
    name: "httpbin",
    url: "https://httpbin.org/status/200",
    intervalSeconds: 120,
    icon: Zap,
    blurb: "Always answers 200 OK",
  },
];

const STEPS = [
  { icon: Plus, title: "Add an endpoint", body: "A URL and how often to check it. That's the whole setup." },
  { icon: Timer, title: "We poll it for you", body: "A background scheduler pings it on your interval, around the clock." },
  { icon: LineChart, title: "Watch it over time", body: "Uptime, latency trends, outage counts and recovery times." },
];

// Static numbers on purpose: this is a labelled preview of the real list, not fake data
// pretending to be live.
const PREVIEW_ROWS = [
  { name: "Payments API", url: "https://api.acme.io/health", status: "HEALTHY", ms: 142, uptime: 99.9,
    spark: [160, 148, 152, 139, 144, 130, 141, 138, 142] },
  { name: "Auth service", url: "https://auth.acme.io/ping", status: "SLOW", ms: 812, uptime: 97.4,
    spark: [320, 410, 505, 640, 588, 712, 690, 780, 812] },
  { name: "Search cluster", url: "https://search.acme.io/_health", status: "DOWN", ms: null, uptime: 91.2,
    spark: [210, 190, 240, 205, 260, 380, 640, 900, 1200] },
];

/**
 * The zero state. A dashboard with nothing on it is the first thing a new user sees, so
 * it does three jobs instead of just apologising for being empty: explains what the tool
 * does, gets a real service added in one click, and shows what the populated dashboard
 * will look like.
 */
export default function GettingStarted({ onAddPreset, onOpenForm }) {
  const [pendingUrl, setPendingUrl] = useState(null);
  const [presetError, setPresetError] = useState(null);

  const handlePreset = (preset) => {
    setPendingUrl(preset.url);
    setPresetError(null);
    onAddPreset({ name: preset.name, url: preset.url, intervalSeconds: preset.intervalSeconds })
      .catch((err) => setPresetError(err.message || "Could not add that service"))
      .finally(() => setPendingUrl(null));
  };

  return (
    <div className="animate-slide-up space-y-6">
      {/* --- Hero ------------------------------------------------------------- */}
      <section className="card relative overflow-hidden border-border-strong">
        <div className="absolute inset-0 bg-gradient-to-br from-brand-600/15 via-transparent to-accent-500/10" />

        <div className="relative grid gap-8 p-8 sm:p-10 lg:grid-cols-[1.35fr,1fr] lg:items-center">
          <div>
            <span className="chip border-brand-500/30 bg-brand-500/10 text-brand-fg">
              <Radar className="h-3.5 w-3.5" />
              Nothing monitored yet
            </span>

            <h2 className="mt-4 text-3xl font-semibold tracking-tight text-text-primary sm:text-4xl">
              Point it at an API and{" "}
              <span className="gradient-text">never guess again</span>
            </h2>

            <p className="mt-3 max-w-xl text-sm leading-relaxed text-text-secondary">
              Add any HTTP endpoint and this dashboard checks it on a schedule, records every
              response, and turns that history into uptime percentages, latency trends and
              outage reports.
            </p>

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <button onClick={onOpenForm} className="btn-primary">
                <Plus className="h-4 w-4" />
                Add your first service
              </button>
              <button
                type="button"
                onClick={() =>
                  document.getElementById("quick-start")?.scrollIntoView({ behavior: "smooth", block: "start" })
                }
                className="btn-secondary"
              >
                Or start from a template
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Decorative radar: concentric rings with staggered pulses, echoing the polling
              loop the scheduler is running in the background. */}
          <div className="relative hidden h-52 items-center justify-center lg:flex" aria-hidden="true">
            <div className="absolute h-44 w-44 rounded-full border border-border" />
            <div className="absolute h-28 w-28 rounded-full border border-border" />
            <div className="absolute h-14 w-14 rounded-full border border-border" />
            {[0, 1, 2].map((i) => (
              <span
                key={i}
                className="absolute h-14 w-14 animate-pulse-ring rounded-full bg-brand-500/25"
                style={{ animationDelay: `${i * 0.66}s` }}
              />
            ))}
            <div className="relative flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-accent-500 shadow-glow">
              <Radar className="h-5 w-5 text-white" />
            </div>
            <span className="absolute right-6 top-10 h-2 w-2 animate-float rounded-full bg-success" />
            <span
              className="absolute bottom-12 left-8 h-2 w-2 animate-float rounded-full bg-accent-400"
              style={{ animationDelay: "1.5s" }}
            />
          </div>
        </div>

        {/* Steps strip */}
        <div className="relative grid divide-y divide-border border-t border-border sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          {STEPS.map((step, i) => (
            <div key={step.title} className="flex gap-3 p-5">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-500/10 text-brand-fg">
                <step.icon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-medium text-text-primary">
                  <span className="mr-1.5 text-text-muted">{i + 1}.</span>
                  {step.title}
                </p>
                <p className="mt-1 text-xs leading-relaxed text-text-secondary">{step.body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* --- One-click templates ---------------------------------------------- */}
      <section id="quick-start" className="scroll-mt-6">
        <div className="mb-3 flex items-end justify-between gap-4">
          <div>
            <p className="eyebrow">Quick start</p>
            <h3 className="mt-1 text-sm font-semibold text-text-primary">
              Add a working endpoint in one click
            </h3>
          </div>
          <p className="hidden text-xs text-text-muted sm:block">You can remove these any time</p>
        </div>

        {presetError && (
          <div className="mb-3 rounded-lg border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">
            {presetError}
          </div>
        )}

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {PRESETS.map((preset) => {
            const busy = pendingUrl === preset.url;
            return (
              <button
                key={preset.url}
                onClick={() => handlePreset(preset)}
                disabled={pendingUrl != null}
                className="card card-interactive group flex flex-col items-start gap-3 p-4 text-left disabled:cursor-not-allowed disabled:opacity-60"
              >
                <div className="flex w-full items-center justify-between">
                  <div className="rounded-lg bg-surface-hover p-2 text-text-secondary transition-colors group-hover:bg-brand-500/10 group-hover:text-brand-fg">
                    <preset.icon className="h-4 w-4" />
                  </div>
                  {busy ? (
                    <Loader2 className="h-4 w-4 animate-spin text-brand-fg" />
                  ) : (
                    <Plus className="h-4 w-4 text-text-muted transition-colors group-hover:text-brand-fg" />
                  )}
                </div>

                <div className="w-full">
                  <p className="text-sm font-medium text-text-primary">{preset.name}</p>
                  <p className="mt-0.5 text-xs text-text-secondary">{preset.blurb}</p>
                  <p className="mt-2 truncate font-mono text-[11px] text-text-muted">{preset.url}</p>
                </div>

                <span className="chip !px-2 !py-0.5 !text-[11px]">
                  <Timer className="h-3 w-3" />
                  every {preset.intervalSeconds}s
                </span>
              </button>
            );
          })}
        </div>
      </section>

      {/* --- Preview of the real thing ---------------------------------------- */}
      <section>
        <div className="mb-3">
          <p className="eyebrow">Preview</p>
          <h3 className="mt-1 text-sm font-semibold text-text-primary">
            What this page looks like once you're monitoring
          </h3>
        </div>

        <div className="card relative overflow-hidden">
          {/* Not interactive and clearly labelled - sample data, not a live row. */}
          <div className="pointer-events-none select-none opacity-70">
            <div className="grid gap-4 border-b border-border p-5 sm:grid-cols-3">
              <PreviewStat label="Avg uptime" value="96.2%" icon={TrendingUp} tone="text-success" />
              <PreviewStat label="Avg response" value="477 ms" icon={Zap} tone="text-brand-fg" />
              <PreviewStat label="Outages (30d)" value="3" icon={Radar} tone="text-danger" />
            </div>

            {PREVIEW_ROWS.map((row) => (
              <div key={row.name} className="flex items-center gap-4 border-b border-border px-5 py-4 last:border-b-0">
                <LiveDot status={row.status} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-text-primary">{row.name}</p>
                  <p className="truncate font-mono text-[11px] text-text-muted">{row.url}</p>
                </div>
                <Sparkline
                  values={row.spark}
                  width={72}
                  height={24}
                  stroke={row.status === "DOWN" ? "rgb(239 68 68)" : row.status === "SLOW" ? "rgb(245 158 11)" : "rgb(34 197 94)"}
                  className="hidden sm:block"
                />
                <div className="hidden w-20 text-right text-sm tabular-nums text-text-secondary sm:block">
                  {row.ms != null ? `${row.ms} ms` : "—"}
                </div>
                <div className="hidden w-24 md:block">
                  <p className="text-right text-xs tabular-nums text-text-secondary">{row.uptime}%</p>
                  <Meter value={row.uptime} tone={row.uptime >= 99 ? "success" : row.uptime >= 95 ? "warning" : "danger"} className="mt-1.5" />
                </div>
              </div>
            ))}
          </div>

          {/* Fade the sample out at the bottom so it never reads as real content. */}
          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-surface-raised to-transparent" />
          <span className="absolute right-4 top-4 chip !text-[10px] uppercase tracking-wide">Sample data</span>
        </div>
      </section>
    </div>
  );
}

function PreviewStat({ label, value, icon: Icon, tone }) {
  return (
    <div className="flex items-center gap-3">
      <div className={`rounded-lg bg-surface-hover p-2 ${tone}`}>
        <Icon className="h-4 w-4" />
      </div>
      <div>
        <p className="text-xs text-text-muted">{label}</p>
        <p className="text-lg font-semibold tracking-tight text-text-primary">{value}</p>
      </div>
    </div>
  );
}
