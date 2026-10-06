package com.businessmanager.backend.dashboard.controller;

import com.businessmanager.backend.dashboard.dto.DashboardAlertsDto;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import com.businessmanager.backend.dashboard.dto.DashboardResponseDto;
import com.businessmanager.backend.dashboard.repository.DashboardConfigRepository;
import com.businessmanager.backend.dashboard.service.DashboardAlertsService;
import com.businessmanager.backend.dashboard.service.DashboardService;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardAlertsService dashboardAlertsService;
    private final DashboardConfigRepository dashboardConfigRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(Authentication authentication) {
        String username = (authentication != null && authentication.getName() != null) 
                ? authentication.getName() 
                : "admin";

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

        List<Long> roleIds = (user != null && user.getRoles() != null)
                ? user.getRoles().stream().map(role -> role.getId()).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> enabledWidgets = roleIds.isEmpty() 
                ? Collections.emptyList() 
                : dashboardConfigRepository.findEnabledWidgetCodesByRoleIds(roleIds);

        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics();
        DashboardAlertsDto alerts = dashboardAlertsService.getAlertsAndRecentActivity();

        DashboardResponseDto response = DashboardResponseDto.builder()
                .enabledWidgets(enabledWidgets)
                .metrics(metrics)
                .alerts(alerts)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardResponseDto> getDashboardStats(Authentication authentication) {
        return getDashboard(authentication);
    }
}
