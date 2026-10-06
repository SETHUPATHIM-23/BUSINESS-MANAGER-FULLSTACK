package com.businessmanager.backend.reports.repository;

import com.businessmanager.backend.reports.entity.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {
}
