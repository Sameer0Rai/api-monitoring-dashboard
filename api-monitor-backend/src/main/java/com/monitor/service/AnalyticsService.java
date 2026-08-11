package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.dto.projection.AnalyticsAggregate;
import com.monitor.dto.response.AnalyticsResponse;
import com.monitor.exception.ValidationException;
import com.monitor.model.ApiLog;
import com.monitor.repository.ApiLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical analytics (uptime/downtime, response-time extremes, outage count, average
 * recovery time) computed from stored {@code ApiLog} rows rather than live polling.
 *
 * <p>Response-time min/max/avg/uptime come from a single DB-side aggregate query
 * (see {@link com.monitor.repository.ApiLogRepository#aggregateAnalytics}). Outage count
 * and recovery time need to detect *transitions* between consecutive checks (success -&gt;
 * failure -&gt; success again), which isn't expressible as a simple SQL aggregate without a
 * database-specific window-function query - so this deliberately fetches the ordered log
 * window into the JVM and scans it once, in a single pass, to find those transitions. That
 * scan is bounded by {@code windowDays}, not "all history ever", which keeps it appropriate
 * for a self-hosted monitoring tool's actual data volume rather than something that
 * silently stops scaling.</p>
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ApiServiceService apiServiceService;
    private final ApiLogRepository logRepository;
    private final MonitorProperties properties;

    public AnalyticsService(ApiServiceService apiServiceService,
                             ApiLogRepository logRepository,
                             MonitorProperties properties) {
        this.apiServiceService = apiServiceService;
        this.logRepository = logRepository;
        this.properties = properties;
    }

    public AnalyticsResponse analyticsFor(Long serviceId, Long ownerId, Integer requestedWindowDays) {
        apiServiceService.findByIdOrThrow(serviceId, ownerId);

        int windowDays = resolveWindowDays(requestedWindowDays);
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        AnalyticsAggregate aggregate = logRepository.aggregateAnalytics(serviceId, since);
        long total = aggregate.totalChecks();
        long successCount = aggregate.successCount() == null ? 0L : aggregate.successCount();
        double uptime = total == 0 ? 0.0 : (successCount * 100.0 / total);
        double downtime = total == 0 ? 0.0 : (100.0 - uptime);

        OutageStats outageStats = computeOutageStats(serviceId, since);

        return new AnalyticsResponse(
                serviceId,
                windowDays,
                total,
                aggregate.averageResponseTimeMs(),
                aggregate.minResponseTimeMs(),
                aggregate.maxResponseTimeMs(),
                uptime,
                downtime,
                outageStats.count(),
                outageStats.averageRecoverySeconds()
        );
    }

    private int resolveWindowDays(Integer requested) {
        if (requested == null) {
            return properties.analytics().defaultWindowDays();
        }
        int min = properties.analytics().minWindowDays();
        int max = properties.analytics().maxWindowDays();
        if (requested < min || requested > max) {
            throw new ValidationException("windowDays", "must be between " + min + " and " + max + " days");
        }
        return requested;
    }

    /**
     * Single forward pass over time-ordered logs within the window: an outage "starts"
     * the moment a check fails after a success; it "ends" the moment a check succeeds
     * again, and the gap between those two timestamps is one recovery-time sample.
     * {@code outageCount} counts outages that *overlap* the window - including one that
     * was already in progress when the window started - not just ones whose failing
     * transition happens to fall inside it.
     *
     * <p>The pass is seeded with the last check strictly <em>before</em> {@code since} (if
     * any) rather than starting from "unknown". Without that seed, a service that was
     * already down when the window started would have its first in-window log
     * misclassified as a brand new outage beginning exactly at the window boundary -
     * an artifact of where the window happens to be drawn, not a real event. Seeding
     * {@code previousSuccess} (and, if that prior check was itself a failure,
     * {@code outageStartedAt}) with the true prior state fixes the count (that carried-over
     * outage is counted once, immediately, rather than re-detected mid-window) and, when it
     * recovers inside the window, the recovery-time sample - measured from when it
     * actually started, not from the window edge.</p>
     */
    private OutageStats computeOutageStats(Long serviceId, Instant since) {
        ApiLog priorToWindow = logRepository
                .findFirstByApiServiceIdAndCheckedAtBeforeOrderByCheckedAtDesc(serviceId, since)
                .orElse(null);

        Boolean previousSuccess = priorToWindow == null ? null : priorToWindow.isSuccess();
        Instant outageStartedAt = (priorToWindow != null && !priorToWindow.isSuccess())
                ? priorToWindow.getCheckedAt()
                : null;

        List<ApiLog> ordered = logRepository.findByApiServiceIdAndCheckedAtAfterOrderByCheckedAtAsc(serviceId, since);

        // outageCount is "outages that overlap the window", not "outages that started
        // inside it" - a carried-over outage (outageStartedAt already seeded above from
        // before the window) counts as 1 here. It will not be double-counted below: the
        // seeded previousSuccess is already false, so the loop's "new failure" branch
        // only fires on an actual success -> failure transition inside the window.
        int outageCount = outageStartedAt != null ? 1 : 0;
        List<Long> recoverySecondsSamples = new ArrayList<>();

        for (ApiLog log : ordered) {
            boolean success = log.isSuccess();

            if (!success && (previousSuccess == null || previousSuccess)) {
                // A genuinely new failure - either the very first check we have any
                // context for, or a transition from a prior success.
                outageCount++;
                outageStartedAt = log.getCheckedAt();
            } else if (success && outageStartedAt != null) {
                recoverySecondsSamples.add(Duration.between(outageStartedAt, log.getCheckedAt()).toSeconds());
                outageStartedAt = null;
            }

            previousSuccess = success;
        }

        Double averageRecoverySeconds = recoverySecondsSamples.isEmpty()
                ? null
                : recoverySecondsSamples.stream().mapToLong(Long::longValue).average().orElse(0.0);

        return new OutageStats(outageCount, averageRecoverySeconds);
    }

    private record OutageStats(int count, Double averageRecoverySeconds) {
    }
}
