import axios from "axios";
import { API_BASE_URL, TOKEN_STORAGE_KEY } from "../config/env";

// Every backend response is wrapped as { success, data, message, timestamp }. The response
// interceptor unwraps that envelope once, here, so the rest of the app just works with
// plain data/errors like it would against any REST API.
const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    if (response.data && typeof response.data.success === "boolean") {
      return { ...response, data: response.data.data };
    }
    return response;
  },
  (error) => {
    const backendMessage = error.response?.data?.message;
    const normalized = new Error(backendMessage || error.message || "Request failed");
    normalized.status = error.response?.status;
    normalized.fieldErrors = error.response?.data?.data ?? null;

    if (normalized.status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      // A hard redirect (not a router navigate) deliberately - this fires from inside an
      // axios interceptor, outside React's render cycle, and a full reload guarantees every
      // in-memory auth/query state resets cleanly rather than leaving stale protected data
      // rendered behind a suddenly-empty auth context.
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    return Promise.reject(normalized);
  }
);

export default client;
