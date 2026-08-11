package com.monitor.controller;

import com.monitor.config.MonitorProperties;
import com.monitor.dto.projection.AnalyticsAggregate;
import com.monitor.dto.response.ApiResponse;
import com.monitor.exception.GlobalExceptionHandler;
import com.monitor.exception.ValidationException;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiLogRepository;
import com.monitor.service.AnalyticsService;
import com.monitor.service.ApiServiceService;
import com.monitor.service.MonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies the full chain a bad {@code limit}/{@code windowDays} query parameter goes
 * through: the service layer rejects it with {@link ValidationException}, and
 * {@link GlobalExceptionHandler} turns that into a clean {@code 400}, not the generic
 * {@code 500} fallback.
 *
 * <p>Deliberately not a {@code @WebMvcTest}/{@code MockMvc} slice: this project uses
 * stateless JWT security, and standing up the real filter chain (or mocking a
 * {@code CustomUserDetails} principal past it) just to assert an exception maps to a
 * status code would be a lot of test-infrastructure weight for logic that is fully
 * exercised by calling the same objects Spring would call - the service methods and the
 * {@code @RestControllerAdvice} - directly.</p>
 */
@ExtendWith(MockitoExtension.class)
class MonitoringControllerValidationTest {

    @Mock
    private ApiServiceService apiServiceService;

    @Mock
    private ApiLogRepository logRepository;

    private MonitoringService monitoringService;
    private AnalyticsService analyticsService;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        MonitorProperties properties = new MonitorProperties(
                new MonitorProperties.Scheduler(15000),
                new MonitorProperties.HealthCheck(5000, 3000, 2, 500, 8, 30000, 60, 15, 3600),
                new MonitorProperties.Status(400),
                new MonitorProperties.Logs(50, 500),
                new MonitorProperties.Analytics(30, 1, 365)
        );
        monitoringService = new MonitoringService(apiServiceService, logRepository, properties);
        analyticsService = new AnalyticsService(apiServiceService, logRepository, properties);

        lenient().when(apiServiceService.findByIdOrThrow(anyLong(), anyLong()))
                .thenReturn(ApiService.builder().id(1L).name("test").url("https://example.test").build());
        lenient().when(logRepository.aggregateAnalytics(anyLong(), any()))
                .thenReturn(new AnalyticsAggregate(0, null, null, null, null));
    }

    @Test
    void invalidLimit_zeroOrNegative_throwsValidationException() {
        assertThatThrownBy(() -> monitoringService.recentLogs(1L, 42L, 0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> monitoringService.recentLogs(1L, 42L, -5))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void invalidWindowDays_outOfConfiguredBounds_throwsValidationException() {
        assertThatThrownBy(() -> analyticsService.analyticsFor(1L, 42L, 0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> analyticsService.analyticsFor(1L, 42L, 1000))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void globalExceptionHandler_mapsValidationExceptionToClean400_forInvalidLimit() {
        ValidationException ex = catchValidationException(() -> monitoringService.recentLogs(1L, 42L, 0));

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).containsKey("limit");
    }

    @Test
    void globalExceptionHandler_mapsValidationExceptionToClean400_forInvalidWindowDays() {
        ValidationException ex = catchValidationException(() -> analyticsService.analyticsFor(1L, 42L, 1000));

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).containsKey("windowDays");
    }

    private static ValidationException catchValidationException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ValidationException ex) {
            return ex;
        }
        throw new AssertionError("Expected a ValidationException to be thrown");
    }
}
