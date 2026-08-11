package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.dto.request.CreateApiServiceRequest;
import com.monitor.exception.DuplicateServiceException;
import com.monitor.exception.ResourceNotFoundException;
import com.monitor.exception.ValidationException;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiServiceRepository;
import com.monitor.repository.UserRepository;
import com.monitor.security.UrlSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for registering and reading monitored services, always scoped to a single
 * owner. Every method here takes the caller's user id explicitly rather than reading it from
 * a thread-local/security context internally - that keeps this class framework-agnostic and
 * easy to unit test with a plain user id, and makes the "is this scoped correctly?" question
 * answerable just by reading each method signature.
 */
@Service
@Transactional(readOnly = true)
public class ApiServiceService {

    private static final Logger log = LoggerFactory.getLogger(ApiServiceService.class);

    private final ApiServiceRepository repository;
    private final UserRepository userRepository;
    private final MonitorProperties properties;
    private final UrlSecurityValidator urlSecurityValidator;

    public ApiServiceService(ApiServiceRepository repository,
                              UserRepository userRepository,
                              MonitorProperties properties,
                              UrlSecurityValidator urlSecurityValidator) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.urlSecurityValidator = urlSecurityValidator;
    }

    public List<ApiService> findAllForOwner(Long ownerId) {
        return repository.findAllByOwnerId(ownerId);
    }

    /**
     * 404s (not 403s) when the service exists but belongs to someone else - this deliberately
     * doesn't reveal whether a given id exists at all to a caller who doesn't own it.
     */
    public ApiService findByIdOrThrow(Long id, Long ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ResourceNotFoundException.forService(id));
    }

    @Transactional
    public ApiService create(CreateApiServiceRequest request, Long ownerId) {
        // Checked before the duplicate lookup - there's no point telling the caller
        // "you already monitor this" for a URL that can't be monitored at all.
        urlSecurityValidator.assertSafeToRegister(request.url());

        if (repository.existsByUrlAndOwnerId(request.url(), ownerId)) {
            throw new DuplicateServiceException(request.url());
        }

        int interval = resolveInterval(request.intervalSeconds());

        ApiService service = ApiService.builder()
                // getReferenceById avoids a SELECT just to populate the FK - Hibernate only
                // needs the id to write api_services.owner_id, not the full User row.
                .owner(userRepository.getReferenceById(ownerId))
                .name(request.name())
                .url(request.url())
                .intervalSeconds(interval)
                .build();

        ApiService saved = repository.save(service);
        log.info("Owner {} registered new monitored service '{}' ({}), interval={}s",
                ownerId, saved.getName(), saved.getUrl(), interval);
        return saved;
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        ApiService service = findByIdOrThrow(id, ownerId);
        repository.delete(service);
        log.info("Deleted monitored service '{}' (id={})", service.getName(), id);
    }

    private int resolveInterval(Integer requested) {
        if (requested == null) {
            return properties.healthCheck().defaultIntervalSeconds();
        }
        int min = properties.healthCheck().minIntervalSeconds();
        int max = properties.healthCheck().maxIntervalSeconds();
        if (requested < min || requested > max) {
            throw new ValidationException(
                    "intervalSeconds", "must be between " + min + " and " + max + " seconds");
        }
        return requested;
    }
}
