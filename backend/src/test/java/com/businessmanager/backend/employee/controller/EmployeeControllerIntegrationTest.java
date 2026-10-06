package com.businessmanager.backend.employee.controller;

import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.employee.dto.*;
import com.businessmanager.backend.employee.entity.Employee;
import com.businessmanager.backend.employee.entity.AttendanceRecord;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.employee.enums.AttendanceStatus;
import com.businessmanager.backend.employee.service.EmployeeService;
import com.businessmanager.backend.employee.service.AttendanceRecordService;
import com.businessmanager.backend.location.entity.Location;
import com.businessmanager.backend.location.repository.LocationRepository;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private EmployeeService employeeService;
    @MockBean private AttendanceRecordService attendanceRecordService;
    @MockBean private LocationRepository locationRepository;
    @MockBean private AccountRepository accountRepository;
    @MockBean private JwtTokenProvider tokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;

    private Employee employee;
    private Location location;
    private Account account;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(10L);
        location.setCode("LOC10");
        location.setName("Branch Office");

        account = new Account();
        account.setId(20L);
        account.setCode("5010");
        account.setName("Wages Expense");

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP001");
        employee.setName("Jane Doe");
        employee.setDepartment("HR");
        employee.setRoleTitle("Recruiter");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setLocation(location);
    }

    // ── Authorization Gating Tests ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getEmployees_NoPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_READ")
    public void getEmployees_HasPermission_Ok() throws Exception {
        when(employeeService.searchEmployees(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(employee)));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_READ")
    public void createEmployee_ReadOnlyUser_Forbidden() throws Exception {
        EmployeeCreateRequest req = EmployeeCreateRequest.builder()
                .employeeCode("EMP002")
                .name("Bob")
                .department("Sales")
                .roleTitle("Sales Exec")
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_WRITE")
    public void createEmployee_WriteUser_Ok() throws Exception {
        EmployeeCreateRequest req = EmployeeCreateRequest.builder()
                .employeeCode("EMP002")
                .name("Bob")
                .department("Sales")
                .roleTitle("Sales Exec")
                .joiningDate(LocalDate.now())
                .locationId(10L)
                .build();

        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(employeeService.createEmployee(any(Employee.class))).thenReturn(employee);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── Validation Errors Tests ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "EMPLOYEE_WRITE")
    public void createEmployee_InvalidRequest_BadRequest() throws Exception {
        EmployeeCreateRequest req = EmployeeCreateRequest.builder()
                .employeeCode("") // Blank
                .name("") // Blank
                .department("Sales")
                .roleTitle("Sales Exec")
                .joiningDate(null) // Missing
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_WRITE")
    public void logAttendance_InvalidTimes_BadRequest() throws Exception {
        AttendanceRecordRequest req = AttendanceRecordRequest.builder()
                .employeeId(1L)
                .date(LocalDate.now())
                .status("PRESENT")
                .checkIn(LocalTime.of(17, 0))
                .checkOut(LocalTime.of(9, 0)) // Check-out before check-in
                .build();

        mockMvc.perform(post("/api/employees/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "EMPLOYEE_WRITE")
    public void getEmployeeDetails_AsHR_ReturnsOk() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(employee);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"));
    }
}
