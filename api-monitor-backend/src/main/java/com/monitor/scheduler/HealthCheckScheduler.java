package com.monitor.scheduler;

import com.monitor.service.HealthCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers a health-check sweep on a fixed interval. All the actual work - concurrency,
 * retries, persistence - lives in {@link HealthCheckService}; this class stays a one-line
 * entry point so the scheduling concern (when) stays separate from the checking concern
 * (how).
 *
 * <p>The interval is {@code monitor.scheduler.fixed-rate-ms} (still one global interval for
 * every service - per-service intervals are a deliberately deferred future feature, see
 * README).</p>
 */
@Component
public class HealthCheckScheduler {

    private final HealthCheckService healthCheckService;

    public HealthCheckScheduler(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @Scheduled(fixedRateString = "${monitor.scheduler.fixed-rate-ms}")
    public void runSweep() {
        healthCheckService.checkAllServices();
    }
}
