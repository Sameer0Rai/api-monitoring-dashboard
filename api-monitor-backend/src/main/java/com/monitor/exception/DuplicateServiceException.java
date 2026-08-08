package com.monitor.exception;

public class DuplicateServiceException extends RuntimeException {

    public DuplicateServiceException(String url) {
        super("A monitored service with url '" + url + "' already exists");
    }
}
