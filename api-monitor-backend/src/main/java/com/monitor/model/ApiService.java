package com.monitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A monitored HTTP endpoint, owned by exactly one {@link User}. This is the persistence
 * entity - external API contracts are represented separately by
 * {@link com.monitor.dto.response.ApiServiceResponse} so that schema changes don't
 * automatically leak into the public API shape.
 */
@Entity
@Table(
        name = "api_services",
        indexes = @Index(name = "idx_api_services_owner", columnList = "owner_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    /** How often this service is checked. Validated against monitor.health-check min/max at request time. */
    @Column(name = "interval_seconds", nullable = false)
    @Builder.Default
    private int intervalSeconds = 60;

    /**
     * Updated after every check. Lets the scheduler ask "which services are due?" with a
     * single indexed comparison instead of a per-service subquery against api_logs.
     */
    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "apiService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiLog> logs = new ArrayList<>();
}
