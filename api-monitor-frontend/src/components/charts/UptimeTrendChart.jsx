import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatTick, formatTooltipLabel } from "../../utils/formatTime";

export default function UptimeTrendChart({ data, height = 240 }) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data}>
        <defs>
          <linearGradient id="uptimeFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#22c55e" stopOpacity={0.35} />
            <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--border))" />
        <XAxis
          dataKey="time"
          tickFormatter={formatTick}
          tick={{ fill: "rgb(var(--text-muted))", fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          minTickGap={40}
        />
        <YAxis
          domain={[0, 1]}
          ticks={[0, 1]}
          tickFormatter={(v) => (v === 1 ? "Up" : "Down")}
          tick={{ fill: "rgb(var(--text-muted))", fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          width={40}
        />
        <Tooltip
          labelFormatter={formatTooltipLabel}
          formatter={(value) => (value === 1 ? "Up" : "Down")}
          contentStyle={{
            background: "rgb(var(--surface-raised))",
            border: "1px solid rgb(var(--border))",
            borderRadius: 8,
            fontSize: 12,
            color: "rgb(var(--text-primary))",
          }}
        />
        <Area type="stepAfter" dataKey="up" stroke="#22c55e" strokeWidth={2} fill="url(#uptimeFill)" name="Status" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
