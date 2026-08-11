import Sparkline from "./Sparkline";

const TONES = {
  default: { icon: "text-brand-fg bg-brand-500/10", accent: "from-brand-500/60", spark: "rgb(129 140 248)" },
  success: { icon: "text-success bg-success/10", accent: "from-success/60", spark: "rgb(34 197 94)" },
  danger: { icon: "text-danger bg-danger/10", accent: "from-danger/60", spark: "rgb(239 68 68)" },
  warning: { icon: "text-warning bg-warning/10", accent: "from-warning/60", spark: "rgb(245 158 11)" },
};

export default function StatCard({
  label,
  value,
  icon: Icon,
  tone = "default",
  subtext,
  trend,
}) {
  const toneStyles = TONES[tone] || TONES.default;

  return (
    <div className="card card-interactive group relative animate-fade-in overflow-hidden p-5">
      {/* Hairline of tone colour along the top edge - identifies the card's meaning at a
          glance when five of them sit in a row. */}
      <div className={`absolute inset-x-0 top-0 h-px bg-gradient-to-r to-transparent ${toneStyles.accent}`} />

      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-text-secondary">{label}</p>
        {Icon && (
          <div className={`rounded-lg p-2 transition-transform group-hover:scale-105 ${toneStyles.icon}`}>
            <Icon className="h-4 w-4" />
          </div>
        )}
      </div>

      <p className="mt-3 text-[28px] font-semibold leading-none tracking-tight text-text-primary">
        {value}
      </p>

      <div className="mt-2 flex items-end justify-between gap-2">
        <p className="text-xs text-text-muted">{subtext || " "}</p>
        {trend?.length > 1 && (
          <Sparkline values={trend} width={64} height={22} stroke={toneStyles.spark} className="shrink-0" />
        )}
      </div>
    </div>
  );
}
