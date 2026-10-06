package com.businessmanager.backend.dashboard.controller;

import com.businessmanager.backend.dashboard.dto.DashboardAlertsDto;
import com.businessmanager.backend.dashboard.dto.DashboardMetricsDto;
import com.businessmanager.backend.dashboard.dto.DashboardResponseDto;
import com.businessmanager.backend.dashboard.repository.DashboardConfigRepository;
import com.businessmanager.backend.dashboard.service.DashboardAlertsService;
import com.businessmanager.backend.dashboard.service.DashboardService;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DashboardAlertsService dashboardAlertsService;

    @Mock
    private DashboardConfigRepository dashboardConfigRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void testGetDashboard_filtersWidgetsByRole() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");

        Role role1 = new Role();
        role1.setId(10L);

        User mockUser = new User();
        mockUser.setUsername("admin");
        mockUser.setRoles(Set.of(role1));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));

        List<String> enabledWidgets = List.of("SALES_KPI", "LOW_STOCK_ALERT");
        when(dashboardConfigRepository.findEnabledWidgetCodesByRoleIds(anyList())).thenReturn(enabledWidgets);

        DashboardMetricsDto metrics = DashboardMetricsDto.builder().build();
        DashboardAlertsDto alerts = DashboardAlertsDto.builder().build();

        when(dashboardService.getDashboardMetrics()).thenReturn(metrics);
        when(dashboardAlertsService.getAlertsAndRecentActivity()).thenReturn(alerts);

        // Act
        ResponseEntity<DashboardResponseDto> response = dashboardController.getDashboard(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEnabledWidgets().size());
        assertEquals("SALES_KPI", response.getBody().getEnabledWidgets().get(0));
        assertEquals("LOW_STOCK_ALERT", response.getBody().getEnabledWidgets().get(1));
        
        // Assert payload is populated
        assertNotNull(response.getBody().getMetrics());
        assertNotNull(response.getBody().getAlerts());
    }
}
