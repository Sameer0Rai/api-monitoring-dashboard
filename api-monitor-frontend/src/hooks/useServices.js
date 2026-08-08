import { useCallback, useEffect, useRef, useState } from "react";
import { createService, getLogs, getMetrics, getServices } from "../api/services";
import { POLL_INTERVAL_MS } from "../config/env";

/**
 * Owns all data-fetching for the dashboard: the service list, each service's recent
 * logs (for the latency chart) and metrics (uptime/status), polled on an interval.
 * Pulling this out of the page component means Dashboard.jsx is just layout, and this
 * hook could be unit-tested (e.g. with @testing-library/react-hooks) independently of
 * any rendering.
 */
export function useServices() {
  const [services, setServices] = useState([]);
  const [logsByService, setLogsByService] = useState({});
  const [metricsByService, setMetricsByService] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Avoids setting state after the component using this hook has unmounted.
  const isMounted = useRef(true);
  useEffect(() => () => { isMounted.current = false; }, []);

  const loadServiceDetails = useCallback((service) => {
    getLogs(service.id)
      .then((res) => {
        if (!isMounted.current) return;
        const points = res.data.map((log, index) => ({
          time: index,
          latency: log.responseTimeMs,
        }));
        setLogsByService((prev) => ({ ...prev, [service.id]: points }));
      })
      .catch(() => {
        // A single service's logs failing to load shouldn't blank out the whole
        // dashboard - the chart for that card just stays empty until the next poll.
      });

    getMetrics(service.id)
      .then((res) => {
        if (!isMounted.current) return;
        setMetricsByService((prev) => ({ ...prev, [service.id]: res.data }));
      })
      .catch(() => {});
  }, []);

  const load = useCallback(() => {
    getServices()
      .then((res) => {
        if (!isMounted.current) return;
        setServices(res.data);
        setError(null);
        res.data.forEach(loadServiceDetails);
      })
      .catch((err) => {
        if (!isMounted.current) return;
        setError(err.message);
      })
      .finally(() => {
        if (isMounted.current) setLoading(false);
      });
  }, [loadServiceDetails]);

  useEffect(() => {
    load();
    const interval = setInterval(load, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [load]);

  const addService = useCallback(
    (name, url) => createService({ name, url }).then(load),
    [load]
  );

  return { services, logsByService, metricsByService, loading, error, addService };
}
