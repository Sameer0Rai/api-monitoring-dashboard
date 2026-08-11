package com.monitor.controller;

import com.monitor.dto.response.AnalyticsResponse;
import com.monitor.dto.response.ApiLogResponse;
import com.monitor.dto.response.ApiMetricsResponse;
import com.monitor.dto.response.ApiResponse;
import com.monitor.mapper.ApiServiceMapper;
import com.monitor.model.ApiLog;
import com.monitor.repository.ApiServiceRepository;
import com.monitor.security.CustomUserDetails;
import com.monitor.service.AnalyticsService;
import com.monitor.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read endpoints for a monitored service's history, computed health, and historical
 * analytics. Every path takes {@code {id}} and is validated against the authenticated
 * caller's own services (see MonitoringService/AnalyticsService - ownership is enforced
 * there, not just here).
 */
@Tag(name = "Monitoring", description = "Logs, live metrics, and historical analytics for your services")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/services")
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final AnalyticsService analyticsService;
    private final ApiServiceRepository apiServiceRepository;

    public MonitoringController(MonitoringService monitoringService,
                                 AnalyticsService analyticsService,
                                 ApiServiceRepository apiServiceRepository) {
        this.monitoringService = monitoringService;
        this.analyticsService = analyticsService;
        this.apiServiceRepository = apiServiceRepository;
    }

    @Operation(summary = "Recent check history for a service (chart-ready, oldest first)")
    @GetMapping("/logs/{id}")
    public ApiResponse<List<ApiLogResponse>> logs(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<ApiLog> logs = monitoringService.recentLogs(id, principal.getId(), limit);
        List<ApiLogResponse> response = logs.stream()
                .map(ApiServiceMapper::toResponse)
                .toList();
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Live uptime/latency/status for a service")
    @GetMapping("/metrics/{id}")
    public ApiResponse<ApiMetricsResponse> metrics(@PathVariable Long id,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        MonitoringService.Metrics metrics = monitoringService.metricsFor(id, principal.getId());
        return ApiResponse.ok(new ApiMetricsResponse(
                id,
                metrics.status(),
                metrics.uptimePercentage(),
                metrics.averageResponseTimeMs(),
                metrics.totalChecks()
        ));
    }

    @Operation(summary = "Historical analytics for a service (uptime/downtime, outages, recovery time)")
    @GetMapping("/{id}/analytics")
    public ApiResponse<AnalyticsResponse> analytics(
            @PathVariable Long id,
            @RequestParam(required = false) Integer windowDays,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(analyticsService.analyticsFor(id, principal.getId(), windowDays));
    }

    @Operation(summary = "Total number of services you're monitoring")
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(Map.of("apis", apiServiceRepository.countByOwnerId(principal.getId())));
    }
}
