export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

/**
 * The backend's own origin, without the `/api` suffix - for links to endpoints that live
 * alongside `/api` but aren't under it (actuator, swagger). `API_BASE_URL` is a full URL
 * in local dev ("http://localhost:8080/api"), but the Docker deployment bakes it to a
 * bare relative path ("/api", proxied by nginx - see nginx.conf) with no scheme or host
 * to strip. In that case those other backend paths only resolve if nginx proxies them
 * too, at the page's own origin - which is exactly what nginx.conf does.
 */
export const API_ORIGIN = /^https?:\/\//i.test(API_BASE_URL)
  ? API_BASE_URL.replace(/\/api\/?$/, "")
  : window.location.origin;

// Every poll fires GET /services plus 2 more requests per monitored service (metrics +
// logs) - on a CPU-limited free-tier host that fan-out adds up fast at a 10s cadence.
// 30s is still fresh enough for a demo without piling up concurrent requests behind it.
export const POLL_INTERVAL_MS = 30000;
export const TOKEN_STORAGE_KEY = "monitor.token";
export const EMAIL_STORAGE_KEY = "monitor.email";
export const THEME_STORAGE_KEY = "monitor.theme";
