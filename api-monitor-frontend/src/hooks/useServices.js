import { useCallback, useEffect, useRef, useState } from "react";
import { createService, deleteService, getLogs, getMetrics, getServices } from "../api/services";
import { POLL_INTERVAL_MS } from "../config/env";

// Enough points for a legible row sparkline without pulling a full history per service
// on every poll.
const SPARKLINE_POINTS = 24;

/**
 * Owns all data-fetching for the dashboard: the service list, each service's live
 * metrics, and a short slice of recent checks for the row sparklines - all polled on an
 * interval. Kept out of the page component so DashboardPage stays layout-only.
 */
export function useServices() {
  const [services, setServices] = useState([]);
  const [metricsByService, setMetricsByService] = useState({});
  const [historyByService, setHistoryByService] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const isMounted = useRef(true);
  useEffect(() => {
    isMounted.current = true;
    return () => { isMounted.current = false; };
  }, []);

  const loadMetrics = useCallback((service) => {
    getMetrics(service.id)
      .then((res) => {
        if (!isMounted.current) return;
        setMetricsByService((prev) => ({ ...prev, [service.id]: res.data }));
      })
      .catch(() => {
        // A single service's metrics failing to load shouldn't blank out the whole
        // dashboard - that row just shows an "Unknown" status until the next poll.
      });
  }, []);

  const loadHistory = useCallback((service) => {
    getLogs(service.id, SPARKLINE_POINTS)
      .then((res) => {
        if (!isMounted.current) return;
        // Logs arrive oldest-first, which is the order the sparkline draws in.
        setHistoryByService((prev) => ({ ...prev, [service.id]: res.data }));
      })
      .catch(() => {
        // Purely decorative on this page - a failure just means no sparkline for now.
      });
  }, []);

  const load = useCallback(() => {
    getServices()
      .then((res) => {
        if (!isMounted.current) return;
        setServices(res.data);
        setError(null);
        res.data.forEach((service) => {
          loadMetrics(service);
          loadHistory(service);
        });
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
  }, [loadMetrics, loadHistory]);

  useEffect(() => {
    load();
    const interval = setInterval(load, POLL_INTERVAL_MS);

    // Chrome/Firefox throttle setInterval heavily in background tabs, so switching away
    // and back can make polling look stalled for a while - refreshing immediately on
    // refocus closes that gap instead of waiting out the throttled interval.
    const onVisible = () => {
      if (document.visibilityState === "visible") load();
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [load]);

  const addService = useCallback(
    (payload) => createService(payload).then(load),
    [load]
  );

  const removeService = useCallback(
    (id) => deleteService(id).then(load),
    [load]
  );

  return {
    services,
    metricsByService,
    historyByService,
    loading,
    error,
    lastUpdatedAt,
    addService,
    removeService,
    refresh: load,
  };
}
