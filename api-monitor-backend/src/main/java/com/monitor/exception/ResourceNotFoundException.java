package com.monitor.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forService(Long id) {
        return new ResourceNotFoundException("No monitored service found with id " + id);
    }
}
