package com.businessmanager.backend.common.exception;

/**
 * Thrown when a requested resource (like a Customer, Product, or Invoice) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
