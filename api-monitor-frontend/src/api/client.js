import axios from "axios";
import { API_BASE_URL } from "../config/env";

// Every backend response is wrapped as { success, data, message, timestamp }.
// This interceptor unwraps that envelope once, here, so the rest of the app
// just works with plain data/errors like it would against any REST API.
const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
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
    return Promise.reject(normalized);
  }
);

export default client;
