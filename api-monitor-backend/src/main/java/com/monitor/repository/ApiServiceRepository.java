package com.monitor.repository;

import com.monitor.model.ApiService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiServiceRepository extends JpaRepository<ApiService, Long> {

    boolean existsByUrl(String url);
}
