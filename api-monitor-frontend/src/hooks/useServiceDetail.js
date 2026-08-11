import { useCallback, useEffect, useRef, useState } from "react";
import { getAnalytics, getLogs, getMetrics, getService } from "../api/services";
import { POLL_INTERVAL_MS } from "../config/env";

export function useServiceDetail(serviceId, windowDays) {
  const [service, setService] = useState(null);
  const [logs, setLogs] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const isMounted = useRef(true);
  useEffect(() => {
    isMounted.current = true;
    return () => { isMounted.current = false; };
  }, []);

  const load = useCallback(() => {
    Promise.all([
      getService(serviceId),
      getLogs(serviceId, 100),
      getMetrics(serviceId),
      getAnalytics(serviceId, windowDays),
    ])
      .then(([serviceRes, logsRes, metricsRes, analyticsRes]) => {
        if (!isMounted.current) return;
        setService(serviceRes.data);
        setLogs(logsRes.data);
        setMetrics(metricsRes.data);
        setAnalytics(analyticsRes.data);
        setError(null);
      })
      .catch((err) => {
        if (!isMounted.current) return;
        setError(err.message);
      })
      .finally(() => {
        if (!isMounted.current) return;
        setLoading(false);
        setLastUpdatedAt(Date.now());
      });
  }, [serviceId, windowDays]);

  useEffect(() => {
    setLoading(true);
    load();
    const interval = setInterval(load, POLL_INTERVAL_MS);

    const onVisible = () => {
      if (document.visibilityState === "visible") load();
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [load]);

  return { service, logs, metrics, analytics, loading, error, lastUpdatedAt, refresh: load };
}
