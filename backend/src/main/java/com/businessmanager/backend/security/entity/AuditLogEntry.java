package com.businessmanager.backend.security.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLogEntry extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "module_name", nullable = false, length = 30)
    private String moduleName;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    // The legacy timestamp column remains in the DB for backward compatibility,
    // but JPA will now manage created_at via BaseEntity.
}
