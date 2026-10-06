package com.businessmanager.backend.dashboard.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.security.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dashboard_configs")
@Getter
@Setter
@NoArgsConstructor
public class DashboardConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "widget_code", nullable = false)
    private String widgetCode;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
