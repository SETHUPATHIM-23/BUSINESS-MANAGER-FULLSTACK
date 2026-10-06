package com.businessmanager.backend.dashboard.service;

import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.dashboard.entity.DashboardConfig;
import com.businessmanager.backend.dashboard.repository.DashboardConfigRepository;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardConfigServiceImpl implements DashboardConfigService {

    private final DashboardConfigRepository dashboardConfigRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoleWidgets(Long roleId) {
        return dashboardConfigRepository.findByRoleId(roleId).stream()
                .filter(DashboardConfig::getIsEnabled)
                .map(DashboardConfig::getWidgetCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRoleWidgets(Long roleId, List<String> widgets) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        List<DashboardConfig> existingConfigs = dashboardConfigRepository.findByRoleId(roleId);
        
        // Map of all possible widgets (for simplicity, we assume these are the ones)
        List<String> allPossibleWidgets = List.of(
            "KPI_SALES", "KPI_PURCHASES", "KPI_FUNDS", "KPI_AGING",
            "ALERT_STOCK", "ACTIVITY_FEED", "SYSTEM_STATUS", "KPI_AGING_DETAIL"
        );

        for (int i = 0; i < allPossibleWidgets.size(); i++) {
            String code = allPossibleWidgets.get(i);
            boolean shouldBeEnabled = widgets != null && widgets.contains(code);

            DashboardConfig config = existingConfigs.stream()
                    .filter(c -> c.getWidgetCode().equals(code))
                    .findFirst()
                    .orElseGet(() -> {
                        DashboardConfig newConfig = new DashboardConfig();
                        newConfig.setRole(role);
                        newConfig.setWidgetCode(code);
                        newConfig.setSortOrder(existingConfigs.size() + 1);
                        return newConfig;
                    });

            config.setIsEnabled(shouldBeEnabled);
            dashboardConfigRepository.save(config);
        }
    }
}
