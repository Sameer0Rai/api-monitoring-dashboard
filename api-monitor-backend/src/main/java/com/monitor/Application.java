package com.monitor;

import com.monitor.config.MonitorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the API Monitoring Dashboard backend.
 *
 * <p>{@link EnableScheduling} activates the fixed-rate health-check job in
 * {@link com.monitor.scheduler.HealthCheckScheduler}. {@link MonitorProperties}
 * binds every tunable (timeouts, retry policy, status thresholds, scheduler rate)
 * from configuration instead of leaving them as magic numbers in code.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MonitorProperties.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
