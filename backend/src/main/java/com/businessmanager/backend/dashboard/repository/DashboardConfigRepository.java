package com.businessmanager.backend.dashboard.repository;

import com.businessmanager.backend.dashboard.entity.DashboardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardConfigRepository extends JpaRepository<DashboardConfig, Long> {

    @Query("SELECT c.widgetCode FROM DashboardConfig c " +
           "WHERE c.role.id IN :roleIds AND c.isEnabled = true " +
           "ORDER BY c.sortOrder ASC")
    List<String> findEnabledWidgetCodesByRoleIds(@Param("roleIds") List<Long> roleIds);

    List<DashboardConfig> findByRoleId(Long roleId);
}
