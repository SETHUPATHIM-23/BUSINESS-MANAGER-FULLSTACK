package com.businessmanager.backend.employee.mapper;

import com.businessmanager.backend.employee.dto.EmployeeResponseDto;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.security.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmployeeMapperTest {

    private final EmployeeMapper employeeMapper = Mappers.getMapper(EmployeeMapper.class);

    private Employee employee;
    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP001");
        employee.setName("John Doe");
        employee.setDepartment("Engineering");
        employee.setRoleTitle("Software Engineer");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));

        // Mock static utility class
        mockedSecurityUtils = Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void toResponseDto_ReturnsBasicFields() {
        EmployeeResponseDto dto = employeeMapper.toResponseDto(employee);

        assertNotNull(dto);
        assertEquals("EMP001", dto.getEmployeeCode());
        assertEquals("John Doe", dto.getName());
    }
}
