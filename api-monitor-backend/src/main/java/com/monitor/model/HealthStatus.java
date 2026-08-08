package com.monitor.model;

/**
 * Classification of a service's current health, derived from recent check results.
 * Thresholds that decide which bucket a service falls into live in
 * {@link com.monitor.config.MonitorProperties.Status} rather than being hardcoded here.
 */
public enum HealthStatus {
    HEALTHY,
    SLOW,
    DOWN,
    UNKNOWN // no checks recorded yet
}
