package com.businessmanager.backend.common.audit.aspect;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.audit.entity.AuditLogEntry;
import com.businessmanager.backend.common.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAuditAction(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            String username = getCurrentUsername();
            String entityId = extractEntityId(result, joinPoint.getArgs());
            String beforeValue = extractBeforeValue(joinPoint.getArgs());
            String afterValue = serializeToJson(result);

            AuditLogEntry logEntry = AuditLogEntry.builder()
                    .username(username)
                    .actionType(auditAction.action())
                    .moduleName(auditAction.module())
                    .entityId(entityId)
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to write audit log entry", e);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "system";
    }

    private String extractEntityId(Object result, Object[] args) {
        if (result != null) {
            String id = getValueOfIdField(result);
            if (id != null) return id;
        }
        
        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof Integer || arg instanceof String) {
                return arg.toString();
            }
            if (arg != null) {
                String id = getValueOfIdField(arg);
                if (id != null) return id;
            }
        }
        return null;
    }

    private String getValueOfIdField(Object obj) {
        try {
            Method getIdMethod = obj.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(obj);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception ignored) {
        }
        
        try {
            java.lang.reflect.Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(obj);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }

    private String extractBeforeValue(Object[] args) {
        for (Object arg : args) {
            if (arg != null && !(arg instanceof Long || arg instanceof Integer || arg instanceof String || arg instanceof Boolean)) {
                return serializeToJson(arg);
            }
        }
        return null;
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON for auditing", e);
            return obj.toString();
        }
    }
}
