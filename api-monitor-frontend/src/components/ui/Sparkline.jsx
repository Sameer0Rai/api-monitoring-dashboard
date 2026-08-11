import { useId, useMemo } from "react";

/**
 * Tiny inline-SVG trend line for table rows and stat cards. Deliberately not Recharts:
 * at 80x24px the tooltips/axes/legends that make Recharts worth it are all disabled
 * anyway, and rendering one Recharts tree per service row is a lot of DOM for a squiggle.
 */
export default function Sparkline({
  values = [],
  width = 88,
  height = 26,
  stroke = "rgb(99 102 241)",
  className = "",
}) {
  const gradientId = useId();

  const { line, area } = useMemo(() => {
    const points = values.filter((v) => typeof v === "number" && Number.isFinite(v));
    if (points.length < 2) return { line: null, area: null };

    const min = Math.min(...points);
    const max = Math.max(...points);
    // A flat series would divide by zero; render it as a centred straight line instead.
    const span = max - min || 1;
    const padY = 3;
    const usableHeight = height - padY * 2;

    const coords = points.map((value, i) => {
      const x = (i / (points.length - 1)) * width;
      const y = padY + (1 - (value - min) / span) * usableHeight;
      return [Number(x.toFixed(2)), Number(y.toFixed(2))];
    });

    const path = coords.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x},${y}`).join(" ");
    return {
      line: path,
      area: `${path} L${width},${height} L0,${height} Z`,
    };
  }, [values, width, height]);

  if (!line) {
    return (
      <div
        className={`flex items-center ${className}`}
        style={{ width, height }}
        aria-hidden="true"
      >
        <div className="h-px w-full bg-border" />
      </div>
    );
  }

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className={className}
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={stroke} stopOpacity="0.28" />
          <stop offset="100%" stopColor={stroke} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#${gradientId})`} />
      <path
        d={line}
        fill="none"
        stroke={stroke}
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
