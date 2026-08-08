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
 * A monitored HTTP endpoint. This is the persistence entity - external API contracts are
 * represented separately by {@link com.monitor.dto.response.ApiServiceResponse} so that
 * changes to the table (e.g. adding an owner_id column in a future auth phase) don't
 * automatically leak into the public API shape.
 */
@Entity
@Table(name = "api_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "apiService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiLog> logs = new ArrayList<>();
}
