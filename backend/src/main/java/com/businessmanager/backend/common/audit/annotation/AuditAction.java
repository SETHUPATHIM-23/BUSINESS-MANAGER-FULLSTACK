package com.businessmanager.backend.common.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method execution should be logged in the Audit Log.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    
    String action(); // e.g. CREATE, UPDATE, DELETE, POST
    
    String module(); // e.g. CUSTOMER, SUPPLIER, BILLING
}
