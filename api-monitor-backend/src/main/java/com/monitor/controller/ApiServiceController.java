package com.monitor.controller;

import com.monitor.dto.request.CreateApiServiceRequest;
import com.monitor.dto.response.ApiResponse;
import com.monitor.dto.response.ApiServiceResponse;
import com.monitor.mapper.ApiServiceMapper;
import com.monitor.model.ApiService;
import com.monitor.service.ApiServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Registration and listing of monitored services.
 * Same two endpoints as the original controller ({@code GET /api/services},
 * {@code POST /api/services}) - only the internals changed (DTOs, validation, service layer).
 */
@RestController
@RequestMapping("/api/services")
public class ApiServiceController {

    private final ApiServiceService apiServiceService;

    public ApiServiceController(ApiServiceService apiServiceService) {
        this.apiServiceService = apiServiceService;
    }

    @GetMapping
    public ApiResponse<List<ApiServiceResponse>> list() {
        List<ApiServiceResponse> services = apiServiceService.findAll().stream()
                .map(ApiServiceMapper::toResponse)
                .toList();
        return ApiResponse.ok(services);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiServiceResponse> create(@Valid @RequestBody CreateApiServiceRequest request) {
        ApiService created = apiServiceService.create(request);
        return ApiResponse.ok(ApiServiceMapper.toResponse(created));
    }
}
