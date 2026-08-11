/**
 * Inline "nothing here" placeholder. No card chrome of its own - it's always rendered
 * inside a panel that already has a border, and nesting two cards looks like a bug.
 */
export default function EmptyState({ icon: Icon, title, description, action, className = "" }) {
  return (
    <div
      className={`flex animate-fade-in flex-col items-center justify-center gap-3 px-6 py-14 text-center ${className}`}
    >
      {Icon && (
        <div className="rounded-full bg-brand-500/10 p-3 text-brand-fg ring-1 ring-inset ring-brand-500/20">
          <Icon className="h-5 w-5" />
        </div>
      )}
      <div>
        <p className="text-sm font-medium text-text-primary">{title}</p>
        {description && (
          <p className="mx-auto mt-1 max-w-sm text-sm text-text-secondary">{description}</p>
        )}
      </div>
      {action}
    </div>
  );
}
