package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.model.ApiLog;
import com.monitor.model.HealthStatus;
import com.monitor.repository.ApiLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@code MonitoringService.classify}, the logic fixed in this pass: current status
 * comes from the single latest check, not an average latency across all history.
 */
@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private ApiServiceService apiServiceService;

    @Mock
    private ApiLogRepository logRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        MonitorProperties properties = new MonitorProperties(
                new MonitorProperties.Scheduler(15000),
                new MonitorProperties.HealthCheck(5000, 3000, 2, 500, 8, 30000, 60, 15, 3600),
                new MonitorProperties.Status(400), // slowThresholdMs = 400
                new MonitorProperties.Logs(50, 500),
                new MonitorProperties.Analytics(30, 1, 365)
        );
        monitoringService = new MonitoringService(apiServiceService, logRepository, properties);
    }

    @Test
    void noLatestLog_isUnknown() {
        assertThat(monitoringService.classify(null)).isEqualTo(HealthStatus.UNKNOWN);
    }

    @Test
    void latestCheckFailed_isDown_regardlessOfLatency() {
        ApiLog fastButFailed = logAt(50, false);
        assertThat(monitoringService.classify(fastButFailed)).isEqualTo(HealthStatus.DOWN);

        ApiLog slowAndFailed = logAt(5000, false);
        assertThat(monitoringService.classify(slowAndFailed)).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    void latestCheckSucceeded_belowSlowThreshold_isHealthy() {
        ApiLog fastSuccess = logAt(120, true); // threshold is 400ms
        assertThat(monitoringService.classify(fastSuccess)).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void latestCheckSucceeded_aboveSlowThreshold_isSlow() {
        ApiLog slowSuccess = logAt(900, true); // threshold is 400ms
        assertThat(monitoringService.classify(slowSuccess)).isEqualTo(HealthStatus.SLOW);
    }

    @Test
    void latestCheckSucceeded_exactlyAtThreshold_isHealthy() {
        // classify() uses a strict ">" comparison - exactly at the threshold is still healthy.
        ApiLog atThreshold = logAt(400, true);
        assertThat(monitoringService.classify(atThreshold)).isEqualTo(HealthStatus.HEALTHY);
    }

    private static ApiLog logAt(long responseTimeMs, boolean success) {
        return ApiLog.builder()
                .responseTimeMs(responseTimeMs)
                .success(success)
                .statusCode(success ? 200 : 0)
                .checkedAt(Instant.now())
                .build();
    }
}
