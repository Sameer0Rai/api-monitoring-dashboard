import Sidebar from "./Sidebar";
import Topbar from "./Topbar";

export default function AppShell({ title, subtitle, icon, children }) {
  return (
    <div className="flex h-screen bg-surface">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar title={title} subtitle={subtitle} icon={icon} />
        <main className="app-backdrop flex-1 overflow-y-auto p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
