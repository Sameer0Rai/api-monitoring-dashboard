package com.monitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One health-check result for a given {@link ApiService}.
 *
 * <p>Two changes from the original schema:</p>
 * <ul>
 *   <li>{@code apiService} is now a real {@code @ManyToOne} foreign key instead of a bare
 *       {@code Long serviceId} - the database now enforces referential integrity, and JPA
 *       can express "logs for this service" as a join instead of an app-level filter.</li>
 *   <li>{@code success} is computed and stored once at write time instead of every reader
 *       re-deriving it from {@code statusCode == 200}, which was both duplicated logic and
 *       wrong for any 2xx that isn't exactly 200.</li>
 * </ul>
 *
 * <p>The composite index on {@code (api_service_id, checked_at)} matches the access pattern
 * this table actually sees: "give me the last N logs for service X", which is the only query
 * shape used by {@link com.monitor.repository.ApiLogRepository}.</p>
 */
@Entity
@Table(
        name = "api_logs",
        indexes = @Index(
                name = "idx_api_logs_service_checked_at",
                columnList = "api_service_id, checked_at"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_service_id", nullable = false)
    private ApiService apiService;

    /** HTTP status code returned, or 0 if the request failed outright (timeout, DNS, refused). */
    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
