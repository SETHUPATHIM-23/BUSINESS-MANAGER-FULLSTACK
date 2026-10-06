package com.businessmanager.backend.truck.controller;

import com.businessmanager.backend.billing.service.InvoiceService;
import com.businessmanager.backend.employee.service.EmployeeService;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.businessmanager.backend.truck.dto.TruckCreateRequest;
import com.businessmanager.backend.truck.dto.TruckExpensePostingRequest;
import com.businessmanager.backend.truck.entity.Truck;
import com.businessmanager.backend.truck.mapper.DeliveryAssignmentMapper;
import com.businessmanager.backend.truck.mapper.MaintenanceLogMapper;
import com.businessmanager.backend.truck.mapper.TruckMapper;
import com.businessmanager.backend.truck.service.DeliveryAssignmentService;
import com.businessmanager.backend.truck.service.MaintenanceLogService;
import com.businessmanager.backend.truck.service.TruckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TruckControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private TruckService truckService;
    @MockBean private DeliveryAssignmentService deliveryAssignmentService;
    @MockBean private MaintenanceLogService maintenanceLogService;
    @MockBean private EmployeeService employeeService;
    @MockBean private InvoiceService invoiceService;
    @MockBean private JwtTokenProvider tokenProvider;
    @MockBean private CustomUserDetailsService userDetailsService;

    private Truck truck;

    @BeforeEach
    void setUp() {
        truck = new Truck();
        truck.setId(1L);
        truck.setRegistrationNumber("TRK-999");
        truck.setMake("Scania");
        truck.setModel("R500");
        truck.setCapacity(20.0);
        truck.setFuelType("DIESEL");
        truck.setLastServiceDate(LocalDate.now().minusDays(100));
    }

    // ── AUTHORIZATION GATING TESTS ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getTrucks_NoPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TRUCK_READ")
    public void getTrucks_HasPermission_Ok() throws Exception {
        when(truckService.searchTrucks(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(truck)));

        mockMvc.perform(get("/api/trucks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "TRUCK_READ")
    public void createTruck_ReadOnlyUser_Forbidden() throws Exception {
        TruckCreateRequest req = TruckCreateRequest.builder()
                .registrationNumber("TRK-123")
                .make("Volvo")
                .model("FH")
                .capacity(15.0)
                .build();

        mockMvc.perform(post("/api/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TRUCK_WRITE")
    public void createTruck_WriteUser_Ok() throws Exception {
        TruckCreateRequest req = TruckCreateRequest.builder()
                .registrationNumber("TRK-123")
                .make("Volvo")
                .model("FH")
                .capacity(15.0)
                .build();

        when(truckService.createTruck(any(Truck.class))).thenReturn(truck);

        mockMvc.perform(post("/api/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── VALIDATION ERROR TESTS ──────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRUCK_WRITE")
    public void createTruck_InvalidRequest_BadRequest() throws Exception {
        TruckCreateRequest req = TruckCreateRequest.builder()
                .registrationNumber("") // Blank
                .make("") // Blank
                .model("") // Blank
                .capacity(-5.0) // Invalid capacity
                .build();

        mockMvc.perform(post("/api/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(authorities = "TRUCK_WRITE")
    public void postTruckExpense_InvalidAmount_BadRequest() throws Exception {
        TruckExpensePostingRequest req = TruckExpensePostingRequest.builder()
                .truckId(1L)
                .expenseAccountId(10L)
                .paymentAccountId(20L)
                .amount(new BigDecimal("-100.00")) // Negative amount
                .date(LocalDate.now())
                .expenseType("FUEL")
                .build();

        mockMvc.perform(post("/api/trucks/post-expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── MAINTENANCE-DUE & EXPENSE POSTING END-TO-END FLOW ───────────────

    @Test
    @WithMockUser(authorities = "TRUCK_READ")
    public void getTrucksDueForMaintenance_ReturnsFlaggedTrucks() throws Exception {
        when(truckService.getTrucksDueForMaintenance(any())).thenReturn(Collections.singletonList(truck));

        mockMvc.perform(get("/api/trucks/maintenance-due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("TRK-999"))
                .andExpect(jsonPath("$[0].maintenanceDue").value(true));
    }

    @Test
    @WithMockUser(authorities = "TRUCK_WRITE")
    public void postTruckExpense_EndToEnd_Ok() throws Exception {
        TruckExpensePostingRequest req = TruckExpensePostingRequest.builder()
                .truckId(1L)
                .expenseAccountId(10L)
                .paymentAccountId(20L)
                .amount(new BigDecimal("450.00"))
                .date(LocalDate.now())
                .expenseType("MAINTENANCE")
                .memo("Oil filter change")
                .build();

        doNothing().when(truckService).postTruckExpense(any(TruckExpensePostingRequest.class));

        mockMvc.perform(post("/api/trucks/post-expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(truckService, times(1)).postTruckExpense(any(TruckExpensePostingRequest.class));
    }
}
