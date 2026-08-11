package com.monitor.repository;

import com.monitor.model.ApiService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiServiceRepository extends JpaRepository<ApiService, Long> {

    List<ApiService> findAllByOwnerId(Long ownerId);

    Optional<ApiService> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByUrlAndOwnerId(String url, Long ownerId);

    /**
     * Used by the summary endpoint - a COUNT query instead of loading every row just to
     * call .size() on the resulting list.
     */
    long countByOwnerId(Long ownerId);
}
