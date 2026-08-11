import client from "./client";

export const registerAccount = (email, password) =>
  client.post("/auth/register", { email, password });

export const login = (email, password) =>
  client.post("/auth/login", { email, password });

export const forgotPassword = (email) =>
  client.post("/auth/forgot-password", { email });

export const resetPassword = (token, newPassword) =>
  client.post("/auth/reset-password", { token, newPassword });
