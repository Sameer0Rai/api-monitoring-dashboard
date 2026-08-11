package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.dto.projection.AnalyticsAggregate;
import com.monitor.dto.response.AnalyticsResponse;
import com.monitor.exception.ValidationException;
import com.monitor.model.ApiLog;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Covers the outage-counting fix in this pass: {@code outageCount} must include an outage
 * that overlaps the window's start (carried over from before it), not just outages whose
 * failing transition happens to occur inside the window - while still measuring recovery
 * time from the outage's true start, even when that's before the window.
 *
 * <p>Every test stubs the two log-fetching repository calls directly (fully controlling
 * "what happened before the window" vs "what happened inside it") rather than relying on
 * real wall-clock timing, so the suite is deterministic regardless of when it runs.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long SERVICE_ID = 1L;
    private static final Long OWNER_ID = 42L;

    @Mock
    private ApiServiceService apiServiceService;

    @Mock
    private ApiLogRepository logRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MonitorProperties properties = new MonitorProperties(
                new MonitorProperties.Scheduler(15000),
                new MonitorProperties.HealthCheck(5000, 3000, 2, 500, 8, 30000, 60, 15, 3600),
                new MonitorProperties.Status(400),
                new MonitorProperties.Logs(50, 500),
                new MonitorProperties.Analytics(30, 1, 365)
        );
        analyticsService = new AnalyticsService(apiServiceService, logRepository, properties);

        when(apiServiceService.findByIdOrThrow(eq(SERVICE_ID), eq(OWNER_ID)))
                .thenReturn(mockService());
        // lenient(): the windowDays-bounds-validation tests throw before this is ever
        // reached, which would otherwise trip Mockito's strict "unnecessary stubbing"
        // check for those specific tests even though every other test in this class does
        // exercise it.
        lenient().when(logRepository.aggregateAnalytics(eq(SERVICE_ID), any(Instant.class)))
                .thenReturn(new AnalyticsAggregate(0, null, null, null, null));
    }

    @Test
    void normalSuccessFailureRecoverySequence_countsOneOutageWithRecoveryTime() {
        // Nothing before the window.
        noPriorLog();

        Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
        List<ApiLog> window = List.of(
                logAt(t0, true),
                logAt(t0.plusSeconds(60), false),   // outage starts
                logAt(t0.plusSeconds(120), false),  // still down
                logAt(t0.plusSeconds(180), true)    // recovered - 120s after it started
        );
        stubWindowLogs(window);

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 7);

        assertThat(result.totalOutages()).isEqualTo(1);
        assertThat(result.averageRecoverySeconds()).isEqualTo(120.0);
    }

    @Test
    void outageBeginningBeforeWindow_recoveringInsideWindow_countsAsOneOutageWithTrueRecoveryTime() {
        // Was already down 500 seconds before the window starts.
        Instant since = Instant.parse("2024-01-08T00:00:00Z");
        Instant trueOutageStart = since.minusSeconds(500);
        priorLog(logAt(trueOutageStart, false));

        List<ApiLog> window = List.of(
                logAt(since.plusSeconds(10), false),  // still down - must NOT count as a new outage
                logAt(since.plusSeconds(700), true)   // recovers inside the window
        );
        stubWindowLogs(window);

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 7);

        assertThat(result.totalOutages()).isEqualTo(1);
        // Recovery measured from the TRUE start (before the window: 500 + 700 = 1200s),
        // not from the window edge.
        assertThat(result.averageRecoverySeconds()).isEqualTo(1200.0);
    }

    @Test
    void outageBeginningInsideWindow_stillOngoingAtWindowEnd_countsButHasNoRecoverySample() {
        noPriorLog();

        Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
        List<ApiLog> window = List.of(
                logAt(t0, true),
                logAt(t0.plusSeconds(60), false) // fails and never recovers within the window
        );
        stubWindowLogs(window);

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 7);

        assertThat(result.totalOutages()).isEqualTo(1);
        assertThat(result.averageRecoverySeconds()).isNull();
    }

    @Test
    void multipleOutages_areAllCounted() {
        noPriorLog();

        Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
        List<ApiLog> window = List.of(
                logAt(t0, true),
                logAt(t0.plusSeconds(60), false),
                logAt(t0.plusSeconds(90), true),   // recovery 1: 30s
                logAt(t0.plusSeconds(200), false),
                logAt(t0.plusSeconds(260), true)   // recovery 2: 60s
        );
        stubWindowLogs(window);

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 7);

        assertThat(result.totalOutages()).isEqualTo(2);
        assertThat(result.averageRecoverySeconds()).isEqualTo(45.0); // (30 + 60) / 2
    }

    @Test
    void carriedOverOutageIsNotDoubleCounted_whenWindowStartsWithMoreFailures() {
        // Regression guard for the specific bug this pass fixed: previously the first
        // in-window log was misread as a brand-new outage even when it was really a
        // continuation of one already in progress.
        Instant since = Instant.parse("2024-01-08T00:00:00Z");
        priorLog(logAt(since.minusSeconds(300), false));

        List<ApiLog> window = List.of(
                logAt(since.plusSeconds(10), false),
                logAt(since.plusSeconds(20), false),
                logAt(since.plusSeconds(30), false)
        );
        stubWindowLogs(window);

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 7);

        assertThat(result.totalOutages()).isEqualTo(1); // not 4
    }

    @Test
    void windowDaysWithinBounds_isAccepted() {
        noPriorLog();
        stubWindowLogs(List.of());

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 90);

        assertThat(result.windowDays()).isEqualTo(90);
    }

    @Test
    void windowDaysBelowMinimum_isRejected() {
        assertThatThrownBy(() -> analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void windowDaysAboveMaximum_isRejected() {
        assertThatThrownBy(() -> analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, 400))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullWindowDays_usesConfiguredDefault() {
        noPriorLog();
        stubWindowLogs(List.of());

        AnalyticsResponse result = analyticsService.analyticsFor(SERVICE_ID, OWNER_ID, null);

        assertThat(result.windowDays()).isEqualTo(30); // properties.analytics().defaultWindowDays()
    }

    // --- helpers ---

    private void noPriorLog() {
        when(logRepository.findFirstByApiServiceIdAndCheckedAtBeforeOrderByCheckedAtDesc(eq(SERVICE_ID), any(Instant.class)))
                .thenReturn(Optional.empty());
    }

    private void priorLog(ApiLog log) {
        when(logRepository.findFirstByApiServiceIdAndCheckedAtBeforeOrderByCheckedAtDesc(eq(SERVICE_ID), any(Instant.class)))
                .thenReturn(Optional.of(log));
    }

    private void stubWindowLogs(List<ApiLog> logs) {
        when(logRepository.findByApiServiceIdAndCheckedAtAfterOrderByCheckedAtAsc(eq(SERVICE_ID), any(Instant.class)))
                .thenReturn(logs);
    }

    private static ApiLog logAt(Instant checkedAt, boolean success) {
        return ApiLog.builder()
                .checkedAt(checkedAt)
                .success(success)
                .statusCode(success ? 200 : 0)
                .responseTimeMs(100)
                .build();
    }

    private static ApiService mockService() {
        return ApiService.builder().id(SERVICE_ID).name("test").url("https://example.test").build();
    }
}
