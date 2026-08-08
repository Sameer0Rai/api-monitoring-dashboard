package com.monitor.controller;

import com.monitor.dto.response.ApiLogResponse;
import com.monitor.dto.response.ApiMetricsResponse;
import com.monitor.dto.response.ApiResponse;
import com.monitor.mapper.ApiServiceMapper;
import com.monitor.model.ApiLog;
import com.monitor.repository.ApiServiceRepository;
import com.monitor.service.MonitoringService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read endpoints for a monitored service's history and computed health.
 * Same three endpoints as the original controller, with the same paths
 * ({@code /api/services/logs/{id}}, {@code /api/services/metrics/{id}},
 * {@code /api/services/summary}) so the frontend contract shape (path structure)
 * doesn't move more than necessary - only the response envelope and field names changed.
 */
@RestController
@RequestMapping("/api/services")
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final ApiServiceRepository apiServiceRepository;

    public MonitoringController(MonitoringService monitoringService,
                                 ApiServiceRepository apiServiceRepository) {
        this.monitoringService = monitoringService;
        this.apiServiceRepository = apiServiceRepository;
    }

    @GetMapping("/logs/{id}")
    public ApiResponse<List<ApiLogResponse>> logs(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit) {

        List<ApiLog> logs = monitoringService.recentLogs(id, limit);
        List<ApiLogResponse> response = logs.stream()
                .map(ApiServiceMapper::toResponse)
                .toList();
        return ApiResponse.ok(response);
    }

    @GetMapping("/metrics/{id}")
    public ApiResponse<ApiMetricsResponse> metrics(@PathVariable Long id) {
        MonitoringService.Metrics metrics = monitoringService.metricsFor(id);
        return ApiResponse.ok(new ApiMetricsResponse(
                id,
                metrics.status(),
                metrics.uptimePercentage(),
                metrics.averageResponseTimeMs(),
                metrics.totalChecks()
        ));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(Map.of("apis", apiServiceRepository.count()));
    }
}
