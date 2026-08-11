import client from "./client";

export const getServices = () => client.get("/services");

export const getService = (id) => client.get(`/services/${id}`);

export const createService = (data) => client.post("/services", data);

export const deleteService = (id) => client.delete(`/services/${id}`);

export const getLogs = (id, limit) =>
  client.get(`/services/logs/${id}`, { params: limit ? { limit } : {} });

export const getMetrics = (id) => client.get(`/services/metrics/${id}`);

export const getAnalytics = (id, windowDays) =>
  client.get(`/services/${id}/analytics`, { params: windowDays ? { windowDays } : {} });
