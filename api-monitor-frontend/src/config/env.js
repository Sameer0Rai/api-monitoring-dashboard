// Single place that reads Vite env vars, so the rest of the app never touches
// `import.meta.env` directly. Vite only exposes vars prefixed with VITE_.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export const POLL_INTERVAL_MS = 5000;
