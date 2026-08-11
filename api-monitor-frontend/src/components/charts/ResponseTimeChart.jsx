import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatTick, formatTooltipLabel } from "../../utils/formatTime";

export default function ResponseTimeChart({ data, height = 240 }) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data}>
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
          tick={{ fill: "rgb(var(--text-muted))", fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          width={40}
          unit="ms"
        />
        <Tooltip
          labelFormatter={formatTooltipLabel}
          contentStyle={{
            background: "rgb(var(--surface-raised))",
            border: "1px solid rgb(var(--border))",
            borderRadius: 8,
            fontSize: 12,
            color: "rgb(var(--text-primary))",
          }}
        />
        <Line type="monotone" dataKey="latency" stroke="#6366f1" strokeWidth={2} dot={false} name="Latency (ms)" />
      </LineChart>
    </ResponsiveContainer>
  );
}
