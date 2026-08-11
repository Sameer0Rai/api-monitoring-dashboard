package com.monitor.scheduler;

import com.monitor.service.HealthCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ticks on a fixed, short interval ({@code monitor.scheduler.fixed-rate-ms}, default 15s)
 * and asks {@link HealthCheckService} which services are actually due for a check based on
 * their own {@code intervalSeconds}. This class stays a one-line entry point so the
 * scheduling concern (when to look) stays separate from the checking concern (what to do).
 */
@Component
public class HealthCheckScheduler {

    private final HealthCheckService healthCheckService;

    public HealthCheckScheduler(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @Scheduled(fixedRateString = "${monitor.scheduler.fixed-rate-ms}")
    public void tick() {
        healthCheckService.checkDueServices();
    }
}
