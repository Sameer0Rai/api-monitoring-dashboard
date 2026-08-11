export function SkeletonCard() {
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between">
        <div className="skeleton h-4 w-24" />
        <div className="skeleton h-8 w-8 rounded-lg" />
      </div>
      <div className="skeleton mt-4 h-7 w-16" />
      <div className="skeleton mt-3 h-3 w-28" />
    </div>
  );
}

export function SkeletonRow() {
  return (
    <div className="flex items-center gap-4 border-b border-border px-5 py-3.5 last:border-b-0">
      <div className="skeleton h-9 w-9 rounded-lg" />
      <div className="flex-1 space-y-2">
        <div className="skeleton h-3.5 w-40" />
        <div className="skeleton h-3 w-56" />
      </div>
      <div className="skeleton hidden h-6 w-20 lg:block" />
      <div className="skeleton hidden h-4 w-14 sm:block" />
      <div className="skeleton h-6 w-20 rounded-full" />
    </div>
  );
}

export function SkeletonChart() {
  return (
    <div className="card p-5">
      <div className="skeleton h-4 w-32" />
      <div className="skeleton mt-4 h-52 rounded-lg" />
    </div>
  );
}
