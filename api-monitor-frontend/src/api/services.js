import client from "./client";

export const getServices = () => client.get("/services");

export const createService = (data) => client.post("/services", data);

export const getLogs = (id, limit) =>
  client.get(`/services/logs/${id}`, { params: limit ? { limit } : {} });

export const getMetrics = (id) => client.get(`/services/metrics/${id}`);
