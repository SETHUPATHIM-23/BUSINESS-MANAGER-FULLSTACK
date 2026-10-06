package com.businessmanager.backend.reports.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.reports.enums.ReportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_definitions")
@Getter
@Setter
public class ReportDefinition extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "report_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "default_filters", columnDefinition = "TEXT")
    private String defaultFilters;

    public String getFilterConfigJson() {
        return this.defaultFilters;
    }

    public void setFilterConfigJson(String filterConfigJson) {
        this.defaultFilters = filterConfigJson;
    }
}
