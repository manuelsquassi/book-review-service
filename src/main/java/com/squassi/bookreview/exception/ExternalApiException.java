package com.squassi.bookreview.exception;

/**
 * Exception thrown when there's an error communicating with external APIs.
 * This provides a clear distinction between internal errors and external service failures.
 */
public class ExternalApiException extends RuntimeException {
    
    private final String serviceName;
    private final String endpoint;
    
    public ExternalApiException(String serviceName, String endpoint, String message) {
        super(String.format("Error calling %s API at %s: %s", serviceName, endpoint, message));
        this.serviceName = serviceName;
        this.endpoint = endpoint;
    }
    
    public ExternalApiException(String serviceName, String endpoint, Throwable cause) {
        super(String.format("Error calling %s API at %s", serviceName, endpoint), cause);
        this.serviceName = serviceName;
        this.endpoint = endpoint;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
}
