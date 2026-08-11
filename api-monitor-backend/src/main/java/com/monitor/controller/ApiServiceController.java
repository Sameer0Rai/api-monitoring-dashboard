package com.monitor.controller;

import com.monitor.dto.request.CreateApiServiceRequest;
import com.monitor.dto.response.ApiResponse;
import com.monitor.dto.response.ApiServiceResponse;
import com.monitor.mapper.ApiServiceMapper;
import com.monitor.model.ApiService;
import com.monitor.security.CustomUserDetails;
import com.monitor.service.ApiServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Registration, listing, and removal of the authenticated user's monitored services.
 * Every method is scoped to {@code principal.getId()} - there is no code path here that
 * can return or modify another user's data.
 */
@Tag(name = "Services", description = "Register and manage your monitored services")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/services")
public class ApiServiceController {

    private final ApiServiceService apiServiceService;

    public ApiServiceController(ApiServiceService apiServiceService) {
        this.apiServiceService = apiServiceService;
    }

    @Operation(summary = "List your monitored services")
    @GetMapping
    public ApiResponse<List<ApiServiceResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        List<ApiServiceResponse> services = apiServiceService.findAllForOwner(principal.getId()).stream()
                .map(ApiServiceMapper::toResponse)
                .toList();
        return ApiResponse.ok(services);
    }

    @Operation(summary = "Get a single monitored service (name, url, interval)")
    @GetMapping("/{id}")
    public ApiResponse<ApiServiceResponse> get(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        ApiService service = apiServiceService.findByIdOrThrow(id, principal.getId());
        return ApiResponse.ok(ApiServiceMapper.toResponse(service));
    }

    @Operation(summary = "Register a new monitored service")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiServiceResponse> create(@Valid @RequestBody CreateApiServiceRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        ApiService created = apiServiceService.create(request, principal.getId());
        return ApiResponse.ok(ApiServiceMapper.toResponse(created));
    }

    @Operation(summary = "Remove a monitored service and its history")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        apiServiceService.delete(id, principal.getId());
    }
}
