package com.businessmanager.backend.common.exception;

/**
 * Thrown when an operation violates a core business rule defined in the SRS.
 */
public class BusinessRuleException extends RuntimeException {
    
    public BusinessRuleException(String message) {
        super(message);
    }
}
