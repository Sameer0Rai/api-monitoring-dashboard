import { Activity, LogOut, Moon, Sun } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";

export default function Topbar({ title, subtitle, icon: Icon }) {
  const { email, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="flex items-center justify-between gap-3 border-b border-border bg-surface-raised/80 px-4 py-3 backdrop-blur sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        {/* The sidebar is hidden below md, so the logo has to live here on mobile. */}
        <div className="rounded-lg bg-gradient-to-br from-brand-500 to-accent-500 p-1.5 md:hidden">
          <Activity className="h-4 w-4 text-white" />
        </div>
        {Icon && (
          <div className="hidden rounded-lg bg-brand-500/10 p-2 text-brand-fg sm:flex">
            <Icon className="h-4 w-4" />
          </div>
        )}
        <div className="min-w-0">
          <h1 className="truncate text-base font-semibold leading-tight text-text-primary sm:text-lg">
            {title}
          </h1>
          {subtitle && <p className="truncate text-xs text-text-muted">{subtitle}</p>}
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <button
          onClick={toggleTheme}
          className="btn-ghost !px-2"
          aria-label="Toggle theme"
          title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
        >
          {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </button>

        <div className="hidden items-center gap-2 rounded-full border border-border bg-surface py-1 pl-1 pr-3 sm:flex">
          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-accent-500 text-[11px] font-semibold text-white">
            {email?.[0]?.toUpperCase() || "?"}
          </div>
          <span className="max-w-[180px] truncate text-xs text-text-secondary">{email}</span>
        </div>

        <button onClick={logout} className="btn-ghost !px-2" aria-label="Log out" title="Log out">
          <LogOut className="h-4 w-4" />
        </button>
      </div>
    </header>
  );
}
