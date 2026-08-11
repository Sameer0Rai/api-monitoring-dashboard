package com.monitor.mapper;

import com.monitor.dto.response.ApiLogResponse;
import com.monitor.dto.response.ApiServiceResponse;
import com.monitor.model.ApiLog;
import com.monitor.model.ApiService;

/**
 * Hand-written entity-to-DTO mapping. A generator like MapStruct would be the right call
 * once there are many entities/DTOs with overlapping fields; with two small, stable shapes
 * like these, a plain static method is easier to read, debug, and step through than an
 * annotation-processor-generated class.
 */
public final class ApiServiceMapper {

    private ApiServiceMapper() {
    }

    public static ApiServiceResponse toResponse(ApiService entity) {
        return new ApiServiceResponse(
                entity.getId(),
                entity.getName(),
                entity.getUrl(),
                entity.getIntervalSeconds(),
                entity.getLastCheckedAt(),
                entity.getCreatedAt()
        );
    }

    public static ApiLogResponse toResponse(ApiLog entity) {
        return new ApiLogResponse(
                entity.getId(),
                entity.getStatusCode(),
                entity.getResponseTimeMs(),
                entity.isSuccess(),
                entity.getCheckedAt()
        );
    }
}
