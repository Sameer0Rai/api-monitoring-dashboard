package com.monitor.dto.projection;

/**
 * Typed result of the aggregate metrics query in {@link com.monitor.repository.ApiLogRepository}.
 *
 * <p>This replaces a previous {@code Object[]} return type, which was the source of a real bug:
 * for a multi-column JPQL query without GROUP BY, Spring Data's result processing wraps the
 * single result row in an extra array before converting it to the declared {@code Object[]}
 * return type - so {@code row[0]} was actually a nested {@code Object[]}, not a {@code Number},
 * and casting it threw {@code ClassCastException}. A JPQL constructor expression
 * ({@code SELECT new ...MetricsAggregate(...)}) sidesteps the whole problem: Hibernate
 * constructs this object directly, with no array indexing or casting involved anywhere.</p>
 *
 * <p>{@code averageResponseTimeMs} and {@code successCount} are {@code null} when there are
 * zero matching logs (SQL {@code AVG}/{@code SUM} over no rows is {@code NULL}); {@code totalChecks}
 * is never null since {@code COUNT} always returns a value, including {@code 0}.</p>
 */
public record MetricsAggregate(
        long totalChecks,
        Double averageResponseTimeMs,
        Long successCount
) {
}
