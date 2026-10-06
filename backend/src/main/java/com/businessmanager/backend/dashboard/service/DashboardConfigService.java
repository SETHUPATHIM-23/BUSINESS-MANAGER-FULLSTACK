package com.businessmanager.backend.dashboard.service;

import java.util.List;

public interface DashboardConfigService {
    List<String> getRoleWidgets(Long roleId);
    void updateRoleWidgets(Long roleId, List<String> widgets);
}
