package com.monitor.service;

import com.monitor.dto.request.CreateApiServiceRequest;
import com.monitor.exception.DuplicateServiceException;
import com.monitor.exception.ResourceNotFoundException;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for registering and reading monitored services. Previously this lived
 * directly in the controller; pulling it out means the controller is now purely concerned
 * with HTTP (status codes, request/response mapping) and this class can be unit-tested
 * without spinning up MockMvc.
 */
@Service
@Transactional(readOnly = true)
public class ApiServiceService {

    private static final Logger log = LoggerFactory.getLogger(ApiServiceService.class);

    private final ApiServiceRepository repository;

    public ApiServiceService(ApiServiceRepository repository) {
        this.repository = repository;
    }

    public List<ApiService> findAll() {
        return repository.findAll();
    }

    public ApiService findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forService(id));
    }

    @Transactional
    public ApiService create(CreateApiServiceRequest request) {
        if (repository.existsByUrl(request.url())) {
            throw new DuplicateServiceException(request.url());
        }

        ApiService service = ApiService.builder()
                .name(request.name())
                .url(request.url())
                .build();

        ApiService saved = repository.save(service);
        log.info("Registered new monitored service '{}' ({})", saved.getName(), saved.getUrl());
        return saved;
    }
}
