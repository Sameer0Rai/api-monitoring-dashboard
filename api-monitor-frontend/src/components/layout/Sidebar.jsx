import { NavLink } from "react-router-dom";
import { Activity, BookOpen, Github, LayoutDashboard, Mail } from "lucide-react";
import { API_ORIGIN } from "../../config/env";

const SWAGGER_URL = `${API_ORIGIN}/swagger-ui.html`;
const CONTACT_EMAIL = "sameer.0rai0@gmail.com";
const GITHUB_URL = "https://github.com/Sameer0Rai";

export default function Sidebar() {
  const linkClasses = ({ isActive }) =>
    `group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
      isActive
        ? "bg-brand-500/10 text-brand-fg"
        : "text-text-secondary hover:bg-surface-hover hover:text-text-primary"
    }`;

  return (
    <aside className="hidden w-60 shrink-0 flex-col border-r border-border bg-surface-raised md:flex">
      <div className="flex items-center gap-2.5 px-5 py-5">
        <div className="rounded-lg bg-gradient-to-br from-brand-500 to-accent-500 p-1.5 shadow-glow">
          <Activity className="h-4 w-4 text-white" />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold text-text-primary">API Monitor</p>
          <p className="text-[10px] uppercase tracking-[0.12em] text-text-muted">Uptime &amp; latency</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 px-3">
        <p className="eyebrow px-3 pb-1.5 pt-2">Monitoring</p>
        <NavLink to="/" end className={linkClasses}>
          {({ isActive }) => (
            <>
              {/* Active marker sits in the sidebar's own edge, so the highlighted item
                  reads as attached to the content area it controls. */}
              <span
                className={`absolute -left-3 h-5 w-0.5 rounded-r bg-brand-400 transition-opacity ${
                  isActive ? "opacity-100" : "opacity-0"
                }`}
              />
              <LayoutDashboard className="h-4 w-4" />
              Dashboard
            </>
          )}
        </NavLink>
      </nav>

      <div className="space-y-3 border-t border-border px-3 py-4">
        <a
          href={SWAGGER_URL}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-text-secondary transition-colors hover:bg-surface-hover hover:text-text-primary"
        >
          <BookOpen className="h-4 w-4" />
          API docs
        </a>
        <p className="px-3 text-[11px] text-text-muted">API Monitoring Dashboard v0.4</p>

        <div className="flex items-center gap-1 px-3">
          <a
            href={`mailto:${CONTACT_EMAIL}`}
            title={CONTACT_EMAIL}
            className="rounded-lg p-1.5 text-text-muted transition-colors hover:bg-surface-hover hover:text-text-primary"
          >
            <Mail className="h-3.5 w-3.5" />
          </a>
          <a
            href={GITHUB_URL}
            target="_blank"
            rel="noreferrer"
            title="GitHub"
            className="rounded-lg p-1.5 text-text-muted transition-colors hover:bg-surface-hover hover:text-text-primary"
          >
            <Github className="h-3.5 w-3.5" />
          </a>
          <p className="px-1 text-[11px] text-text-muted">Made by Sameer Rai</p>
        </div>
      </div>
    </aside>
  );
}
